package com.codeshelf.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codeshelf.edi.IEdiExportServiceProvider;
import com.codeshelf.edi.WorkInstructionCSVExporter;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.HousekeepingInjector;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.WiFactory;
import com.codeshelf.model.WiSetSummary;
import com.codeshelf.model.WiSummarizer;
import com.codeshelf.model.WorkInstructionSequencerABC;
import com.codeshelf.model.WorkInstructionSequencerFactory;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.SingleWorkItem;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.util.CompareNullChecker;
import com.codeshelf.util.UomNormalizer;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.validation.MethodArgumentException;
import com.codeshelf.ws.jetty.protocol.response.GetOrderDetailWorkResponse;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class WorkService extends AbstractCodeshelfExecutionThreadService implements IApiService {

	public static final long			DEFAULT_RETRY_DELAY			= 10000L;
	private static final String			SHUTDOWN_MESSAGE	= "*****SHTUDOWN*****";
	public static final int				DEFAULT_CAPACITY			= Integer.MAX_VALUE;
	private static Double				BAY_ALIGNMENT_FUDGE			= 0.25;

	private static final Logger			LOGGER						= LoggerFactory.getLogger(WorkService.class);
	private BlockingQueue<WIMessage>	completedWorkInstructions;

	@Getter
	@Setter
	private long						retryDelay;

	@Getter
	@Setter
	private int							capacity;

	private IEdiExportServiceProvider	exportServiceProvider;

	@Transient
	private WorkInstructionCSVExporter	wiCSVExporter;

	@ToString
	public static class Work {
		@Getter
		private OrderDetail	outboundOrderDetail;

		@Getter
		private Location	firstLocationOnPath;

		@Getter
		private Container	container;

		public Work(Container container, OrderDetail outboundOrderDetail, Location firstLocationOnPath) {
			super();
			this.container = container;
			this.outboundOrderDetail = outboundOrderDetail;
			this.firstLocationOnPath = firstLocationOnPath;
		}
	}

	public WorkService() {
		this(new IEdiExportServiceProvider() {
			@Override
			public IEdiService getWorkInstructionExporter(Facility facility) {
				return facility.getEdiExportService();
			}
		});
	}

	public WorkService(IEdiExportServiceProvider exportServiceProvider) {
		init(exportServiceProvider);
	}

	private void init(IEdiExportServiceProvider exportServiceProvider) {
		this.exportServiceProvider = exportServiceProvider;
		this.wiCSVExporter = new WorkInstructionCSVExporter();
		this.retryDelay = DEFAULT_RETRY_DELAY;
		this.capacity = DEFAULT_CAPACITY;
	}

	// --------------------------------------------------------------------------
	/**
	 * Compute work instructions for a CHE that's at the listed location with the listed container IDs.
	 *
	 * Yes, this has high cyclometric complexity, but the creation of a WI in a complex puzzle.  If you decompose this logic into
	 * fractured routines then there's a chance that they could get called out of order or in the wrong order, etc.  Sometimes in life
	 * you have a complex process and there's no way to make it simple.
	 *
	 * @param inChe
	 * @param inContainerIdList
	 * @return
	 */
	public final WorkList computeWorkInstructions(final Che inChe, final List<String> inContainerIdList) {
		return computeWorkInstructions(inChe, inContainerIdList, false);
	}

	public final WorkList computeWorkInstructions(final Che inChe, final List<String> inContainerIdList, final Boolean reverse) {
		//List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		inChe.clearChe();

		Facility facility = inChe.getFacility();
		// DEV-492 identify previous container uses
		ArrayList<ContainerUse> priorCntrUses = new ArrayList<ContainerUse>();
		priorCntrUses.addAll(inChe.getUses());
		ArrayList<ContainerUse> newCntrUses = new ArrayList<ContainerUse>();

		// Set new uses on the CHE.
		List<Container> containerList = new ArrayList<Container>();
		for (String containerId : inContainerIdList) {
			Container container = facility.getContainer(containerId);
			if (container != null) {
				// add to the list that will generate work instructions
				containerList.add(container);
				// Set the CHE on the containerUse
				ContainerUse thisUse = container.getCurrentContainerUse();
				if (thisUse != null) {
					newCntrUses.add(thisUse); // DEV-492 bit
					Che previousChe = thisUse.getCurrentChe();
					if (previousChe != null) {
			//			changedChes.add(previousChe);
					}
					if (previousChe == null) {
						inChe.addContainerUse(thisUse);
					} else if (!previousChe.equals(inChe)) {
						previousChe.removeContainerUse(thisUse);
						inChe.addContainerUse(thisUse);
					}

					try {
						ContainerUse.staticGetDao().store(thisUse);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}
			} else {
				LOGGER.warn("Unknown container '{}'", containerId);
			}
		}

		// DEV-492 remove previous container uses.
		// just to avoid a long hang after this first runs against old data with hundreds of stale uses, limit to 50.
		int cleanCount = 0;
		final int lkMostUsesToConsider = 50;
		for (ContainerUse oldUse : priorCntrUses) {
			cleanCount++;
			if (cleanCount >= lkMostUsesToConsider)
				break;
			if (!newCntrUses.contains(oldUse)) {
				inChe.removeContainerUse(oldUse);
				try {
					ContainerUse.staticGetDao().store(oldUse);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}

		Timestamp theTime = now();

		// Get all of the OUTBOUND work instructions.
		WorkList workList = generateOutboundInstructions(facility, inChe, containerList, theTime);
		//wiResultList.addAll(generateOutboundInstructions(facility, inChe, containerList, theTime));

		// Get all of the CROSS work instructions.
		//wiResultList.addAll(generateCrossWallInstructions(facility, inChe, containerList, theTime));
		List<WorkInstruction> crossInstructions = generateCrossWallInstructions(facility, inChe, containerList, theTime);
		workList.getInstructions().addAll(crossInstructions);

		//Filter,Sort, and save actionsable WI's
		//TODO Consider doing this in getWork?
		//sortAndSaveActionableWIs(facility, wiResultList);
		sortAndSaveActionableWIs(facility, workList.getInstructions(), reverse);

		LOGGER.info("TOTAL WIs {}", workList.getInstructions());

		//Return original full list
		return workList;
	}

	private Timestamp now() {
		Timestamp theTime = new Timestamp(System.currentTimeMillis());
		return theTime;
	}


	// just a call through to facility, but convenient for the UI
	public final void fakeSetupUpContainersOnChe(UUID cheId, String inContainers) {
		final boolean doThrowInstead = false;
		Che che = Che.staticGetDao().findByPersistentId(cheId);
		if (che == null)
			return;

		if (doThrowInstead)
			// If you want to test this, change value of doThrowInstead above. Then from the UI, select a CHE and
			// do the testing only, set up containers.  Should need "simulate" login for this, although as of V11, works with "configure".
			// DEV-532 shows what used to happen before the error was caught and the transaction rolled back.

			doIntentionalPersistenceError(che);
		else
			setUpCheContainerFromString(che, inContainers);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inChe
	 * @param inContainers
	 * Testing only!  passs in as 23,46,2341a23. This yields container ID 23 in slot1, container Id 46 in slot 2, etc.
	 *
	 */
	public final WorkList setUpCheContainerFromString(Che inChe, String inContainers) {
		if (inChe == null)
			return null;

		// computeWorkInstructions wants a containerId list
		List<String> containersIdList = Arrays.asList(inContainers.split("\\s*,\\s*")); // this trims out white space

		if (containersIdList.size() > 0) {
			return this.computeWorkInstructions(inChe, containersIdList);
			// That did the work. Big side effect. Deleted existing WIs for the CHE. Made new ones. Assigned container uses to the CHE.
			// The wiCount returned is mainly or convenience and debugging. It may not include some shorts
		}
		return null;
	}

	public void completeWorkInstruction(UUID cheId, WorkInstruction incomingWI) {
		Che che = Che.staticGetDao().findByPersistentId(cheId);
		if (che != null) {
			try {
				final WorkInstruction storedWi = persistWorkInstruction(incomingWI);
				exportWorkInstruction(storedWi);
			} catch (DaoException e) {
				LOGGER.error("Unable to record work instruction: " + incomingWI, e);
			} catch (IOException e) {
				LOGGER.error("Unable to export work instruction: " + incomingWI, e);
			}
		} else {
			throw new IllegalArgumentException("Could not find che for id: " + cheId);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI simulation
	 * @return
	 */
	public final void fakeCompleteWi(String wiPersistentId, String inCompleteStr) {
		WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(wiPersistentId);
		boolean doComplete = inCompleteStr.equalsIgnoreCase("COMPLETE");
		boolean doShort = inCompleteStr.equalsIgnoreCase("SHORT");

		// default to complete values
		Integer actualQuant = wi.getPlanQuantity();
		WorkInstructionStatusEnum newStatus = WorkInstructionStatusEnum.COMPLETE;

		if (doComplete) {
		} else if (doShort) {
			actualQuant--;
			newStatus = WorkInstructionStatusEnum.SHORT;
		}
		Timestamp completeTime = now();
		Timestamp startTime = new Timestamp(System.currentTimeMillis() - (10 * 1000)); // assume 10 seconds earlier

		wi.setActualQuantity(actualQuant);
		wi.setCompleted(completeTime);
		wi.setStarted(startTime);
		wi.setStatus(newStatus);
		wi.setType(WorkInstructionTypeEnum.ACTUAL);

		//send in in like in came from SiteController
		completeWorkInstruction(wi.getAssignedChe().getPersistentId(), wi);
	}

	/**
	 * Provides the list of actual work instruction for the scanned orderdetail
	 * @param inChe
	 * @param inScannedOrderDetailId
	 * @return
	 */
	public GetOrderDetailWorkResponse getWorkInstructionsForOrderDetail(final Che inChe, final String inScannedOrderDetailId) {
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		GetOrderDetailWorkResponse response = new GetOrderDetailWorkResponse();
		if (inChe == null) {
			throw new MethodArgumentException(0, inScannedOrderDetailId, ErrorCode.FIELD_REQUIRED);
		}
		if (inScannedOrderDetailId == null) {
			throw new MethodArgumentException(1, inScannedOrderDetailId, ErrorCode.FIELD_REQUIRED);
		}

		LOGGER.info("getWorkInstructionsForOrderDetail request for " + inChe.getDomainId() + " detail:" + inScannedOrderDetailId);

		Map<String, Object> filterArgs = ImmutableMap.<String,Object>of(
			"facilityId", inChe.getFacility().getPersistentId(),
			"domainId", inScannedOrderDetailId
		);
		List<OrderDetail> orderDetails = OrderDetail.staticGetDao().findByFilter("orderDetailByFacilityAndDomainId", filterArgs);

		if (orderDetails.isEmpty()) {
			// temporary: just return empty list instead of throwing
			response.setStatusMessage("Line Item Not Found");
			response.setWorkInstructions(wiResultList);
			return response;
		}
		if (orderDetails.size() > 1) {
			// temporary: just return empty list instead of throwing
			response.setStatusMessage("Ambiguous Line Item");
			response.setWorkInstructions(wiResultList);
			return response;
		}

		OrderDetail orderDetail = orderDetails.get(0);
		inChe.clearChe();
		@SuppressWarnings("unused")
		Timestamp theTime = now();

		// Pass facility as the default location of a short WI..
		WorkInstruction aWi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
			WorkInstructionTypeEnum.PLAN,
			orderDetail,
			inChe,
			null); // Could be normal WI, or a short WI
		if (aWi != null) {
			wiResultList.add(aWi);
			orderDetail.reevaluateStatus();
		} else if (orderDetail.getStatus() == OrderStatusEnum.COMPLETE) {
			//As of DEV-561 we are adding completed WIs to the list in order to be able
			//give feedback on complete orders (and differentiate a 100% complete order from
			//unknown container id. The computeWork method will filter these out before sorting
			//and saving
			for (WorkInstruction wi : orderDetail.getWorkInstructions()) {
				//As of DEV-603 we are only adding completed WIs to the list
				if (WorkInstructionStatusEnum.COMPLETE == wi.getStatus()) {
					LOGGER.info("Adding already complete WIs to list; orderDetail={}", orderDetail);
					wiResultList.add(wi);
				}
			}
		}

		response.setWorkInstructions(wiResultList);
		return response;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inChe
	 * @param inScannedLocationId
	 * @return
	 * Provides the list of work instruction beyond the current scan location. Implicitly assumes only one path, or more precisely, any work instructions
	 * for that CHE are assumed be on the path of the scanned location.
	 * For testing: if scan location, then just return all work instructions assigned to the CHE. (Assumes no negative positions on path.)
	 */
	public final List<WorkInstruction> getWorkInstructions(final Che inChe, final String inScannedLocationId) {
		return getWorkInstructions(inChe, inScannedLocationId, false, false);
	}

	public final List<WorkInstruction> getWorkInstructions(final Che inChe, final String inScannedLocationId, Boolean reversePickOrder, Boolean reverseOrderFromLastTime) {
		long startTimestamp = System.currentTimeMillis();
		Facility facility = inChe.getFacility();

		//boolean start = CheDeviceLogic.STARTWORK_COMMAND.equalsIgnoreCase(inScannedLocationId);
		//boolean reverse = CheDeviceLogic.REVERSE_COMMAND.equalsIgnoreCase(inScannedLocationId);

		//Get current complete list of WIs
		List<WorkInstruction> completeRouteWiList = findCheInstructionsFromPosition(inChe, 0.0, false);

		//We could have existing HK WIs if we've already retrieved the work instructions once but scanned a new location.
		//In that case, we must make sure we remove all existing HK WIs so that we can properly add them back in at the end.
		//We may want to consider not hitting the database for this. It is easiest/safest option for now.
		for (Iterator<WorkInstruction> wiIter = completeRouteWiList.iterator(); wiIter.hasNext();) {
			WorkInstruction wi = wiIter.next();
			if (wi.isHousekeeping()) {
				LOGGER.info("Removing exisiting HK WI={}", wi);
				WorkInstruction.staticGetDao().delete(wi);
				wiIter.remove();
			}
		}

		//Scanning "start" used to send a "" location here, to getWorkInstructions().
		//Now, we pass "start" or "reverse", instead, but still need to pass "" to the getStartingPathDistance() function below.
		//String locationIdCleaned = (start || reverse) ? "" : inScannedLocationId;
		Double startingPathPos = getStartingPathDistance(facility, inScannedLocationId);

		if (startingPathPos == null) {
			List<WorkInstruction> preferredInstructions = new ArrayList<WorkInstruction>();
			for (WorkInstruction instruction : completeRouteWiList) {
				OrderDetail detail = instruction.getOrderDetail();
				if (detail.isPreferredDetail()){
					preferredInstructions.add(instruction);
				}
			}
			Collections.sort(preferredInstructions, new GroupAndSortCodeComparator());
			if (reverseOrderFromLastTime) {
				preferredInstructions = Lists.reverse(preferredInstructions);
			}
			if (preferredInstructions.size() > 0) {
				preferredInstructions = HousekeepingInjector.addHouseKeepingAndSaveSort(facility, preferredInstructions);
			}

			return preferredInstructions;
		}

		// Get all of the PLAN WIs assigned to this CHE beyond the specified position
		List<WorkInstruction> wiListFromStartLocation = findCheInstructionsFromPosition(inChe, startingPathPos, reversePickOrder);


		// Make sure sorted correctly. The query just got the work instructions.
		Collections.sort(wiListFromStartLocation, new GroupAndSortCodeComparator());

		List<WorkInstruction> wrappedRouteWiList = null;
		if (wiListFromStartLocation.size() == completeRouteWiList.size()) {
			// just use what we had This also covers the case of wiCountCompleteRoute == 0.
			wrappedRouteWiList = wiListFromStartLocation;
		} else {
			LOGGER.debug("Wrapping the CHE route. StartList={} CompleteList={}", wiListFromStartLocation, completeRouteWiList);

			Collections.sort(completeRouteWiList, new GroupAndSortCodeComparator());

			// Add the first ones in order.  Only one missing case. If scan is a valid position, but beyond all work instruction position, then we must
			// "wrap" to the complete list.
			if (wiListFromStartLocation.size() == 0) {
				wrappedRouteWiList = completeRouteWiList;
			} else {
				// normal wrap. Add what we got to the end of the path. Then add on what we would have got if we started from the start.
				wrappedRouteWiList = Lists.newArrayList(wiListFromStartLocation);

				//Remove what we just added from the complete list. This will keep the proper order
				completeRouteWiList.removeAll(wiListFromStartLocation);

				//Add the remaining WIs back into the wrapped list IN ORDER
				wrappedRouteWiList.addAll(completeRouteWiList);
			}
		}

		if (reverseOrderFromLastTime) {
			wrappedRouteWiList = Lists.reverse(wrappedRouteWiList);
		}
		// Now our wrappedRouteWiList is ordered correctly but is missing HouseKeepingInstructions
		if (wrappedRouteWiList.size() > 0) {
			wrappedRouteWiList = HousekeepingInjector.addHouseKeepingAndSaveSort(facility, wrappedRouteWiList);
		}

		//Log time if over 2 seconds
		Long wrapComputeDurationMs = System.currentTimeMillis() - startTimestamp;
		if (wrapComputeDurationMs > 2000) {
			LOGGER.warn("GetWork() took {}; totalWis={};", wrapComputeDurationMs, wrappedRouteWiList.size());
		}
		Timer timer = MetricsService.getInstance().createTimer(MetricsGroup.WSS, "cheWorkFromLocation");
		timer.update(wrapComputeDurationMs, TimeUnit.MILLISECONDS);

		return wrappedRouteWiList;
	}

	private void deleteExistingShortWiToFacility(final OrderDetail inOrderDetail) {
		// Do we have short work instruction already for this orderDetail, for any CHE, going to facility?
		// Note, that leaves the shorts around that a user shorted.  This only delete the shorts created immediately upon scan if there is no product.

		// separate list to delete from, because we get ConcurrentModificationException if we delete in the middle of inOrderDetail.getWorkInstructions()
		List<WorkInstruction> aList = new ArrayList<WorkInstruction>();
		List<WorkInstruction> wis = inOrderDetail.getWorkInstructions();
		for (WorkInstruction wi : wis) {
			if (wi.getStatus() == WorkInstructionStatusEnum.SHORT)
				if (wi.getLocation().isFacility()) { // planned to the facility DEV-609
					aList.add(wi);
				}
		}

		// need a reverse iteration?
		for (WorkInstruction wi : aList) {
			try {
				Che assignedChe = wi.getAssignedChe();
				if (assignedChe != null)
					assignedChe.removeWorkInstruction(wi); // necessary?
				inOrderDetail.removeWorkInstruction(wi); // necessary?
				WorkInstruction.staticGetDao().delete(wi);

			} catch (DaoException e) {
				LOGGER.error("failed to delete prior work SHORT instruction", e);
			}

		}

	}

	/**
	 * toPossibleLocations will return a list, but the list may be empty
	 */
	private List<Location> toPossibleLocations(OrderDetail matchingOutboundOrderDetail, List<Path> paths) {
		ArrayList<Location> locations = new ArrayList<Location>();
		for (Path path : paths) {
			OrderLocation firstOutOrderLoc = matchingOutboundOrderDetail.getParent().getFirstOrderLocationOnPath(path);
			if (firstOutOrderLoc != null)
				locations.add(firstOutOrderLoc.getLocation());
		}
		return locations;
	}

	private List<OrderDetail> toAllMatchingOutboundOrderDetails(List<OrderHeader> allFacilityOrderHeaders,
		OrderHeader crossbatchOrder) {
		List<OrderDetail> allMatchingOrderDetails = Lists.newArrayList();
		for (OrderDetail crossOrderDetail : crossbatchOrder.getOrderDetails()) {
			if (crossOrderDetail.getActive()) {
				List<OrderDetail> matchingOrderDetails = toMatchingOutboundOrderDetail(allFacilityOrderHeaders, crossOrderDetail);
				allMatchingOrderDetails.addAll(matchingOrderDetails);
			}
		}
		return allMatchingOrderDetails;
	}

	private List<OrderDetail> toMatchingOutboundOrderDetail(List<OrderHeader> allFacilityOrderHeaders,
		OrderDetail crossbatchOrderDetail) {
		Preconditions.checkNotNull(crossbatchOrderDetail);
		Preconditions.checkArgument(crossbatchOrderDetail.getActive());
		Preconditions.checkArgument(crossbatchOrderDetail.getParent().getOrderType().equals(OrderTypeEnum.CROSS));

		List<OrderDetail> matchingOutboundOrderDetail = new ArrayList<OrderDetail>();
		for (OrderHeader outOrder : allFacilityOrderHeaders) {
			boolean match = true;
			match &= outOrder.getOrderType().equals(OrderTypeEnum.OUTBOUND);
			match &= outOrder.getActive();
			match &= Objects.equal(crossbatchOrderDetail.getParent().getOrderGroup(), outOrder.getOrderGroup());
			if (match) {
				for (OrderDetail outOrderDetail : outOrder.getOrderDetails()) {
					if (outOrderDetail.getActive()) {
						boolean matchDetail = true;
						matchDetail &= outOrderDetail.getItemMaster().equals(crossbatchOrderDetail.getItemMaster());
						matchDetail &= UomNormalizer.normalizedEquals(outOrderDetail.getUomMasterId(),
							crossbatchOrderDetail.getUomMasterId());
						if (matchDetail) {
							matchingOutboundOrderDetail.add(outOrderDetail);
						}
					}

				}
			}
		}
		return matchingOutboundOrderDetail;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrder
	 * @return
	 */
	private void setWiPickInstruction(WorkInstruction inWi, OrderHeader inOrder) {
		String locationString = "";

		// For DEV-315, if more than one location, sort them.
		List<String> locIdList = new ArrayList<String>();

		for (OrderLocation orderLocation : inOrder.getActiveOrderLocations()) {
			LocationAlias locAlias = orderLocation.getLocation().getPrimaryAlias();
			if (locAlias != null) {
				locIdList.add(locAlias.getAlias());
			} else {
				locIdList.add(orderLocation.getLocation().getLocationId());
			}
		}
		// new way. Not sorted. Simple alpha sort. Will fail on D-10 D-11 D-9
		Collections.sort(locIdList);
		locationString = Joiner.on(" ").join(locIdList);
		// end DEV-315 modification

		inWi.doSetPickInstruction(locationString);

		try {
			WorkInstruction.staticGetDao().store(inWi);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort a list of work instructions on a path through a CrossWall
	 * @param inCrosswallWiList
	 * @param inBays
	 * @return
	 */
	@SuppressWarnings("unused")
	private List<WorkInstruction> sortCrosswallInstructionsInLocationOrder(final List<WorkInstruction> inCrosswallWiList,
		final List<Location> inSubLocations) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		// Cycle over all bays on the path.
		for (Location subLocation : inSubLocations) {
			for (Location workLocation : subLocation.getSubLocationsInWorkingOrder()) {
				Iterator<WorkInstruction> wiIterator = inCrosswallWiList.iterator();
				while (wiIterator.hasNext()) {
					WorkInstruction wi = wiIterator.next();
					if (wi.getLocation().equals(workLocation)) {
						wiResultList.add(wi);
						wi.setGroupAndSortCode(String.format("%04d", wiResultList.size()));
						WorkInstruction.staticGetDao().store(wi);
						wiIterator.remove();
					}
				}
			}
		}
		return wiResultList;
	}

	// --------------------------------------------------------------------------
	/**
	 * Generate pick work instructions for a container at a specific location on a path.
	 * @param inChe
	 * @param inContainerList
	 * @param inTime
	 * @return
	 */
	private WorkList generateOutboundInstructions(final Facility facility,
		final Che inChe,
		final List<Container> inContainerList,
		final Timestamp inTime) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		List<OrderDetail> uncompletedDetails = new ArrayList<OrderDetail>();
		int count = 0;

		// To proceed, there should container use linked to outbound order
		// We want to add all orders represented in the container list because these containers (or for Accu, fake containers representing the order) were scanned for this CHE to do.
		for (Container container : inContainerList) {
			OrderHeader order = container.getCurrentOrderHeader();
			if (order != null && order.getOrderType().equals(OrderTypeEnum.OUTBOUND)) {
				boolean orderDetailChanged = false;
				for (OrderDetail orderDetail : order.getOrderDetails()) {
					if (!orderDetail.getActive()) {
						continue;
					}
					// An order detail might be set to zero quantity by customer, essentially canceling that item. Don't make a WI if canceled.
					if (orderDetail.getQuantity() > 0) {
						count++;
						LOGGER.debug("WI #" + count + "in generateOutboundInstructions");
						try {
							// Pass facility as the default location of a short WI..
							SingleWorkItem workItem = makeWIForOutbound(orderDetail,
								inChe,
								container,
								inTime,
								facility,
								facility.getPaths()); // Could be normal WI, or a short WI
							if (workItem.getDetail() != null) {
								uncompletedDetails.add(workItem.getDetail());
							}
							WorkInstruction aWi = workItem.getInstruction();
							if (aWi != null) {
								wiResultList.add(aWi);
								orderDetailChanged |= orderDetail.reevaluateStatus();
							} else if (orderDetail.getStatus() == OrderStatusEnum.COMPLETE) {
								//As of DEV-561 we are adding completed WIs to the list in order to be able
								//give feedback on complete orders (and differentiate a 100% complete order from
								//unknown container id. The computeWork method will filter these out before sorting
								//and saving
								for (WorkInstruction wi : orderDetail.getWorkInstructions()) {
									//As of DEV-603 we are only adding completed WIs to the list
									if (WorkInstructionStatusEnum.COMPLETE == wi.getStatus()) {
										LOGGER.info("Adding already complete WIs to list; orderDetail={}", orderDetail);
										wiResultList.add(wi);
									}
								}
							}
						}
						catch(DaoException e) {
							LOGGER.error("Unable to create work instruction for che: {}, orderDetail: {}, container: {}", inChe, orderDetail, container, e);
						}
					}
					if (orderDetailChanged) {
						order.reevaluateStatus();
					}

				}
			}
		}
		//return wiResultList;
		WorkList workList = new WorkList();
		workList.setInstructions(wiResultList);
		workList.setDetails(uncompletedDetails);
		return workList;
	}

	// --------------------------------------------------------------------------
	/**
	 * Find all of the OUTBOUND orders that need items held in containers holding CROSS orders.
	 * @param inContainerUse
	 * @param inOrder
	 * @param inPath
	 * @param inCheLocation
	 * @return
	 */
	private List<WorkInstruction> generateCrossWallInstructions(final Facility facility,
		final Che inChe,
		final List<Container> inContainerList,
		final Timestamp inTime) {

		List<WorkInstruction> wiList = Lists.newArrayList();
		for (Container container : inContainerList) {
			BatchResult<Work> result = determineWorkForContainer(facility, container);
			for (Work work : result.getResult()) {
				try {
					WorkInstruction wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
						WorkInstructionTypeEnum.PLAN,
						work.getOutboundOrderDetail(),
						work.getContainer(),
						inChe,
						work.getFirstLocationOnPath(),
						inTime);

					// If we created a WI then add it to the list.
					if (wi != null) {
						setWiPickInstruction(wi, work.getOutboundOrderDetail().getParent());
						wiList.add(wi);
					}
				} catch(DaoException e) {
					LOGGER.error("Unable to create work instruction for: {}", work, e);
				}
			}
		}
		return wiList;
	}

	public BatchResult<Work> determineWorkForContainer(Facility facility, Container container) {
		// Iterate over all active CROSS orders on the path.
		BatchResult<Work> batchResult = new BatchResult<Work>();
		OrderHeader crossOrder = container.getCurrentOrderHeader();
		if ((crossOrder != null) && (crossOrder.getActive()) && (crossOrder.getOrderType().equals(OrderTypeEnum.CROSS))) {
			List<OrderHeader> allOrders = facility.getOrderHeaders();
			List<OrderDetail> matchingOrderDetails = toAllMatchingOutboundOrderDetails(allOrders, crossOrder);
			for (OrderDetail matchingOutboundOrderDetail : matchingOrderDetails) {
				List<Path> allPaths = facility.getPaths();
				List<Location> firstOrderLocationPerPath = toPossibleLocations(matchingOutboundOrderDetail, allPaths);
				for (Location aLocationOnPath : firstOrderLocationPerPath) {
					Work work = new Work(container, matchingOutboundOrderDetail, aLocationOnPath);
					batchResult.add(work);
				} /* for else */
				if (firstOrderLocationPerPath.isEmpty()) {
					batchResult.addViolation("matchingOutboundOrderDetail",
						matchingOutboundOrderDetail,
						"did not have a matching order location on any path");
				}
			} /* for else */
			if (matchingOrderDetails.isEmpty()) {
				batchResult.addViolation("currentOrderHeader", crossOrder, "no matching outbound order detail");
			}
		} else {
			batchResult.addViolation("currentOrderHeader", container.getCurrentOrderHeader(), ErrorCode.FIELD_REFERENCE_INACTIVE);
		}
		return batchResult;
	}

	// --------------------------------------------------------------------------
	/**
	 *Utility function for outbound order WI generation
	 * @param inChe
	 * @param inContainer
	 * @param inTime
	 * @return
	 */
	private SingleWorkItem makeWIForOutbound(final OrderDetail inOrderDetail,
		final Che inChe,
		final Container inContainer,
		final Timestamp inTime,
		final Facility inFacility,
		final List<Path> paths) throws DaoException {

		WorkInstruction resultWi = null;
		SingleWorkItem resultWork = new SingleWorkItem();
		ItemMaster itemMaster = inOrderDetail.getItemMaster();

		// DEV-637 note: The code here only works if there is inventory on a path. If the detail has a workSequence,
		// we can make the work instruction anyway.
		Location location = null;
		String workSeqr = PropertyService.getInstance().getPropertyFromConfig(inFacility, DomainObjectProperty.WORKSEQR);
		if (WorkInstructionSequencerType.WorkSequence.toString().equals(workSeqr)) {
			if (inOrderDetail.getWorkSequence() != null) {
				String preferredLocationStr = inOrderDetail.getPreferredLocation();
				if (!Strings.isNullOrEmpty(preferredLocationStr)) {
					location = inFacility.findLocationById(preferredLocationStr);
					if (location == null) {
						location = inFacility.getUnspecifiedLocation();
					} else if (!location.isActive()){
						LOGGER.warn("Unexpected inactive location for preferred Location: {}", location);
						location = inFacility.getUnspecifiedLocation();
					}					
				} else {
					LOGGER.warn("Wanted workSequence mode but need locationId for detail: {}", inOrderDetail);
				}
			}
		} else { //Bay Distance
			Location preferredLocation = inOrderDetail.getPreferredLocObject();
			if (preferredLocation != null && preferredLocation.getAssociatedPathSegment() != null) {
				location = preferredLocation;
			} else {
				for (Path path : paths) {
					String uomStr = inOrderDetail.getUomMasterId();
					Item item = itemMaster.getFirstActiveItemMatchingUomOnPath(path, uomStr);

					if (item != null) {
						Location itemLocation = item.getStoredLocation();
						location = itemLocation;
						break;
					}
				}
			}
		}


		if (location == null) {


			// Need to improve? Do we already have a short WI for this order detail? If so, do we really want to make another?
			// This should be moderately rare, although it happens in our test case over and over. User has to scan order/container to cart to make this happen.
			deleteExistingShortWiToFacility(inOrderDetail);

			//Based on our preferences, either auto-short an instruction for a detail that can't be found on the path, or don't and add that detail to the list
			if (doAutoShortInstructions()) {
				// If there is no location to send the Selector then create a PLANNED, SHORT WI for this order detail.
				resultWi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.SHORT,
					WorkInstructionTypeEnum.ACTUAL,
					inOrderDetail,
					inContainer,
					inChe,
					inFacility,
					inTime);
				if (resultWi != null) {
					resultWi.setPlanQuantity(0);
					resultWi.setPlanMinQuantity(0);
					resultWi.setPlanMaxQuantity(0);
					WorkInstruction.staticGetDao().store(resultWi);
				}
				resultWork.setInstruction(resultWi);
			} else {
				resultWork.setDetail(inOrderDetail);
			}
		} else {
			resultWi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
				WorkInstructionTypeEnum.PLAN,
				inOrderDetail,
				inContainer,
				inChe,
				location,
				inTime);
				resultWork.setInstruction(resultWi);

		}
		return resultWork;
	}

	private boolean doAutoShortInstructions(){
		return false;
	}

	private void sortAndSaveActionableWIs(Facility facility, List<WorkInstruction> allWIs, Boolean reverse) {
		//Create a copy of the list to prevent unintended side effects from filtering
		allWIs = Lists.newArrayList(allWIs);
		//Now we want to filer/sort and save the work instructions that are actionable

		//Filter out complete WI's
		Iterator<WorkInstruction> iter = allWIs.iterator();
		while (iter.hasNext()) {
			if (iter.next().getStatus() == WorkInstructionStatusEnum.COMPLETE) {
				iter.remove();
			}
		}

		//This will sort and also FILTER out WI's that have no location (i.e. SHORTS)
		//It uses the iterater or remove items from the existing list and add it to the new one
		//If all we care about are the counts. Why do we even sort them now?
		WorkInstructionSequencerABC sequencer = WorkInstructionSequencerFactory.createSequencer(facility);

		List<WorkInstruction> sortedWIResults = sequencer.sort(facility, allWIs);
		if (reverse) {
			sortedWIResults = Lists.reverse(sortedWIResults);
		}

		//Save sort
		WorkInstructionSequencerABC.setSortCodesByCurrentSequence(sortedWIResults);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWorkInstruction
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void exportWorkInstruction(WorkInstruction inWorkInstruction) throws IOException {
		// jr/hibernate  tracking down an error
		if (completedWorkInstructions == null) {
			// if queue not defined, just don't queue it
			LOGGER.trace("null completedWorkInstructions in WorkService.exportWorkInstruction", new Exception());
		} else if (inWorkInstruction == null) {
			LOGGER.error("null input to WorkService.exportWorkInstruction", new Exception());
		} else {
			LOGGER.debug("Queueing work instruction: " + inWorkInstruction);
			String messageBody = wiCSVExporter.exportWorkInstructions(ImmutableList.of(inWorkInstruction));
			Facility facility = inWorkInstruction.getParent();
			IEdiService ediExportService = exportServiceProvider.getWorkInstructionExporter(facility);
			WIMessage wiMessage = new WIMessage(ediExportService, messageBody);
			completedWorkInstructions.add(wiMessage);
		}
	}

	public List<WiSetSummary> workAssignedSummary(UUID cheId, UUID facilityId) {
		WiSummarizer summarizer = new WiSummarizer();
		summarizer.computeAssignedWiSummariesForChe(cheId, facilityId);
		return summarizer.getSummaries();
	}

	public List<WiSetSummary> workCompletedSummary(UUID cheId, UUID facilityId) {
		WiSummarizer summarizer = new WiSummarizer();
		summarizer.computeCompletedWiSummariesForChe(cheId, facilityId);
		return summarizer.getSummaries();
	}

	/**
	 * Simple struct to keep associate export settings
	 * @author pmonteiro
	 *
	 */
	private static class WIMessage {
		private IEdiService	exportService;
		private String		messageBody;

		public WIMessage(IEdiService exportService, String messageBody) {
			super();
			this.exportService = exportService;
			this.messageBody = messageBody;
		}
	}

	private class GroupAndSortCodeComparator implements Comparator<WorkInstruction> {
		// Sort the WIs by their sort code. This is identical to CheDeviceLogic.WiGroupSortComparator

		@Override
		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {

			int value = CompareNullChecker.compareNulls(inWi1, inWi2);
			if (value != 0)
				return value;

			String w1Sort = inWi1.getGroupAndSortCode();
			String w2Sort = inWi2.getGroupAndSortCode();
			value = CompareNullChecker.compareNulls(w1Sort, w2Sort);
			if (value != 0)
				return value;

			return w1Sort.compareTo(w2Sort);
		}
	}

	/**
	 * Helper function used in the context after scan location. Gives the distance, or distance of the bay across the path if within 2% of same distance.
	 * Sort of special case. If null or empty string passed in, return 0.0. But unknown location returns null, which causes empty WI list.
	 * @param inScannedLocationId
	 * Returns null and logs errors for bad input situation
	 */
	private Double getStartingPathDistance(final Facility facility, final String inScannedLocationId) {
		if (inScannedLocationId == null || inScannedLocationId.isEmpty())
			return 0.0;

		Location cheLocation = null;
		cheLocation = facility.findSubLocationById(inScannedLocationId);
		if (cheLocation == null) {
			LOGGER.warn("Unknown CHE scan location={}; This may due to a misconfigured site or bad barcode at the facility.",
				inScannedLocationId);
			return null;
		}

		Double startingPathPos = null;
		if (cheLocation != null) {
			Path path = cheLocation.getAssociatedPathSegment().getParent();
			Bay cheBay = cheLocation.getParentAtLevel(Bay.class);
			Bay selectedBay = cheBay;
			if (cheBay == null) {
				LOGGER.error("Che does not have a bay parent location in getStartingPathDistance #1");
				return null;
			} else if (cheBay.getPosAlongPath() == null) {
				LOGGER.error("Ches bay parent location does not have posAlongPath in getStartingPathDistance #2");
				return null;
			}

			for (Bay bay : path.<Bay> getLocationsByClass(Bay.class)) {
				// Find any bay sooner on the work path that's within 2% of this bay.
				if (bay.getPosAlongPath() == null) {
					LOGGER.error("bay location does not have posAlongPath in getStartingPathDistance #3");
				} else if ((bay.getPosAlongPath() < cheBay.getPosAlongPath())
						&& (bay.getPosAlongPath() + BAY_ALIGNMENT_FUDGE > cheBay.getPosAlongPath())) {
					selectedBay = bay;
				}
			}

			// Figure out the starting path position.
			startingPathPos = selectedBay.getPosAlongPath();
			// subtract 1 cm. KLUDGE, but ok.  The greaterOrEqual failed in the equals case after hibernate conversion.
			// filterParams.add(Restrictions.ge("posAlongPath", inFromStartingPosition));
			startingPathPos -= 0.01;
		}
		return startingPathPos;
	}

	/**
	 * Helper function used in the context after scan location. Assumes computeWorkinstructions was already done.
	 * @param inChe
	 * @param inFromStartingPosition
	 * @param inWiList
	 * May return empty list, but never null
	 */
	private List<WorkInstruction> findCheInstructionsFromPosition(final Che inChe, final Double inFromStartingPosition, final Boolean getBeforePosition) {
		List<WorkInstruction> cheWorkInstructions = new ArrayList<>();
		if (inChe == null || inFromStartingPosition == null) {
			LOGGER.error("null input to queryAddCheInstructionsToList");
			return cheWorkInstructions;
		}


		Collection<WorkInstructionTypeEnum> wiTypes = new ArrayList<WorkInstructionTypeEnum>(3);
		wiTypes.add(WorkInstructionTypeEnum.PLAN);
		wiTypes.add(WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		wiTypes.add(WorkInstructionTypeEnum.HK_REPEATPOS);

		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("assignedChe", inChe));
		filterParams.add(Restrictions.in("type", wiTypes));
		if (getBeforePosition) {
			filterParams.add(Restrictions.le("posAlongPath", inFromStartingPosition));
		} else {
			filterParams.add(Restrictions.ge("posAlongPath", inFromStartingPosition));
		}

		//String filter = "(assignedChe.persistentId = :chePersistentId) and (typeEnum = :type) and (posAlongPath >= :pos)";
		//throw new NotImplementedException("Needs to be implemented with a custom query");

		// Hibernate version has test failing with database lock here, so pull out the query
		List<WorkInstruction> filterWiList = WorkInstruction.staticGetDao().findByFilter(filterParams);

		for (WorkInstruction wi : filterWiList) {
			// Very unlikely. But if some wLocationABCs were deleted between start work and scan starting location, let's not give out the "deleted" wis
			// Note: puts may have had multiple order locations, now quite denormalized on WI fields and hard to decompose.  We just take the first as the WI location.
			// Not ambiguous for picks.
			Location loc = wi.getLocation();
			// so far, wi must have a location. Even housekeeping and shorts
			if (loc == null)
				LOGGER.error("getWorkInstructions found active work instruction with null location"); // new log message from v8. Don't expect any null.
			else if (loc.isActive()) //unlikely that location got deleted between complete work instructions and scan location
				cheWorkInstructions.add(wi);
			else
				LOGGER.warn("getWorkInstructions found active work instruction in deleted locations"); // new from v8
		}
		return cheWorkInstructions;
	}

	/**
	 * Support method to force an exception for testing
	 */
	private void doIntentionalPersistenceError(Che che) {
		String desc = "";
		for (int count = 0; count < 500; count++) {
			desc += "X";
		}
		// No try/catch here. The intent is to fail badly and see how the system handles it.
		che.setDescription(desc);
		Che.staticGetDao().store(che);
		LOGGER.warn("Intentional database persistence error. Setting too long description on " + che.getDomainId());
	}

	private WorkInstruction persistWorkInstruction(WorkInstruction updatedWi) throws DaoException {
		UUID wiId = updatedWi.getPersistentId();
		WorkInstruction storedWi = WorkInstruction.staticGetDao().findByPersistentId(wiId);
		if (storedWi == null) {
			throw new InputValidationException(updatedWi, "persistentId", wiId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
		}
		storedWi.setPickerId(updatedWi.getPickerId());
		storedWi.setActualQuantity(updatedWi.getActualQuantity());
		storedWi.setStatus(updatedWi.getStatus());
		storedWi.setType(WorkInstructionTypeEnum.ACTUAL);
		storedWi.setStarted(updatedWi.getStarted());
		storedWi.setCompleted(updatedWi.getCompleted());
		WorkInstruction.staticGetDao().store(storedWi);

		// Find the order detail for this WI and mark it.
		OrderDetail orderDetail = storedWi.getOrderDetail();
		// from v5 housekeeping WI may have null orderDetail
		if (orderDetail != null) {
			orderDetail.reevaluateStatus();
			OrderHeader order = orderDetail.getParent();
			if (order != null) {
				order.reevaluateStatus();
			}
		}
		return storedWi;
	}

	@Override
	protected void run() throws Exception {
		// run
		//serviceThread = Thread.currentThread();
		try {
			boolean shutdownRequested = false;
			while (isRunning() && !shutdownRequested) {
				WIMessage exportMessage = null;
				try {
					exportMessage = completedWorkInstructions.take();
				} catch (InterruptedException e1) {
				}
				if(exportMessage != null) {
					if(exportMessage.messageBody.equals(SHUTDOWN_MESSAGE)) {
						shutdownRequested=true;
					} else {
						processExportMessage(exportMessage);
					}
				}
			}
		} finally {
			//serviceThread = null;
			this.completedWorkInstructions = null;
		}
	}

	private void processExportMessage(WIMessage exportMessage) {
		TenantPersistenceService.getInstance().beginTransaction();
		try {
			//transaction begun and closed after blocking call so that it is not held open
			boolean sent = false;
			while (!sent) {
				try {
					IEdiService ediExportService = exportMessage.exportService;
					ediExportService.sendWorkInstructionsToHost(exportMessage.messageBody);
					sent = true;
				} catch (IOException e) {
					LOGGER.warn("failure to send work instructions, retrying after: " + retryDelay, e);
					Thread.sleep(retryDelay);
				}
			}
			TenantPersistenceService.getInstance().commitTransaction();
		} catch (Exception e) {
			TenantPersistenceService.getInstance().rollbackTransaction();
			LOGGER.error("Unexpected exception sending work instruction, skipping: " + exportMessage, e);
		}
	}

	@Override
	protected void triggerShutdown() {
		WIMessage poison = new WorkService.WIMessage(null,WorkService.SHUTDOWN_MESSAGE);
		this.completedWorkInstructions.offer(poison);
	}

	@Override
	protected void startUp() throws Exception {
		// initialize
		this.completedWorkInstructions = new LinkedBlockingQueue<WIMessage>(this.capacity);
	}

	public boolean willOrderDetailGetWi(OrderDetail inOrderDetail) {
		String sequenceKind = PropertyService.getInstance().getPropertyFromConfig(inOrderDetail.getFacility(), DomainObjectProperty.WORKSEQR);
		WorkInstructionSequencerType sequenceKindEnum = WorkInstructionSequencerType.parse(sequenceKind);

		OrderTypeEnum myParentType = inOrderDetail.getParentOrderType();
		if (myParentType != OrderTypeEnum.OUTBOUND)
			return false;

		// Need to know if this is a simple outbound pick order, or linked to crossbatch.
		OrderDetail matchingCrossDetail = inOrderDetail.outboundDetailToMatchingCrossDetail();
		if (matchingCrossDetail != null) { // Then we only need the outbound order to have a location on the path
			OrderHeader myParent = inOrderDetail.getParent();
			List<OrderLocation> locations = myParent.getOrderLocations();
			if (locations.size() == 0)
				return false;
			// should check non-deleted locations, on path. Not initially.
			return true;

		} else { // No cross detail. Assume outbound pick. Only need inventory on the path. Not checking path/work area now.
			String inventoryLocs = inOrderDetail.getItemLocations();

			if (inOrderDetail.getPreferredLocation() != null &&
					!inOrderDetail.getPreferredLocation().isEmpty()) {

				if ( sequenceKindEnum.equals(WorkInstructionSequencerType.BayDistance)) {
					// If preferred location is set but it is not modeled return false
					// We need to the location to be modeled to compute bay distance
					Location preferredLocation = inOrderDetail.getPreferredLocObject();

					if ( preferredLocation != null
							&& ( preferredLocation.getPathSegment() != null
								|| preferredLocation.getParent().getPathSegment() != null
								|| preferredLocation.getParent().getParent().getPathSegment() != null )
							) {
						return true;
					} else {
						// Check if item is on any valid path
						List<Path> allPaths = inOrderDetail.getFacility().getPaths();
						ItemMaster itemMaster = inOrderDetail.getItemMaster();
						List<Location> itemLocations = new ArrayList<Location>();

						for(Path p : allPaths){
							String uomStr = inOrderDetail.getUomMasterId();
							Item item = itemMaster.getFirstActiveItemMatchingUomOnPath(p, uomStr);

							if (item != null){
								Location itemLocation = item.getStoredLocation();
								itemLocations.add(itemLocation);
								break;
							}
						}

						if (!itemLocations.isEmpty()){
							return true;
						} else {
							return false;
						}
					}

				} else if ( sequenceKindEnum.equals(WorkInstructionSequencerType.WorkSequence)) {
					if ( inOrderDetail.getWorkSequence() != null ) {
						return true;
					} else {
						return false;
					}
				}
			}

			if (!inventoryLocs.isEmpty()) {
				return true;
			}
		}

		// See facility.determineWorkForContainer(Container container) which returns batch results but only for crossbatch situation. That and this should share code.

		return false;
	}
}
