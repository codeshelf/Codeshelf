package com.codeshelf.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.omg.CORBA.BooleanHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.OrderLocationFeedbackMessage;
import com.codeshelf.edi.IEdiExportServiceProvider;
import com.codeshelf.edi.WorkInstructionCSVExporter;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.HousekeepingInjector;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderStatusSummary;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.WiFactory;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.WiSetSummary;
import com.codeshelf.model.WiSummarizer;
import com.codeshelf.model.WorkInstructionSequencerABC;
import com.codeshelf.model.WorkInstructionSequencerFactory;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Slot;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.SingleWorkItem;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.util.CompareNullChecker;
import com.codeshelf.util.UomNormalizer;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.validation.MethodArgumentException;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.response.GetOrderDetailWorkResponse;
import com.codeshelf.ws.protocol.response.GetPutWallInstructionResponse;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class WorkService extends AbstractCodeshelfExecutionThreadService implements IApiService {

	private static final String			THREAD_CONTEXT_TAGS_KEY	= "tags";										// duplicated in CheDeviceLogic. Need a common place

	public static final long			DEFAULT_RETRY_DELAY		= 10000L;
	private static final String			SHUTDOWN_MESSAGE		= "*****SHUTDOWN*****";
	public static final int				DEFAULT_CAPACITY		= Integer.MAX_VALUE;
	private static Double				BAY_ALIGNMENT_FUDGE		= 0.25;

	private static final Logger			LOGGER					= LoggerFactory.getLogger(WorkService.class);
	private BlockingQueue<WIMessage>	completedWorkInstructions;

	private final LightService			lightService;

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

	@Inject
	public WorkService(LightService lightService) {
		this(lightService, new IEdiExportServiceProvider() {
			@Override
			public IEdiService getWorkInstructionExporter(Facility facility) {
				return facility.getEdiExportService();
			}
		});
	}

	public WorkService(LightService lightService, IEdiExportServiceProvider exportServiceProvider) {
		this.lightService = lightService;
		this.exportServiceProvider = exportServiceProvider;
		this.wiCSVExporter = new WorkInstructionCSVExporter();
		this.retryDelay = DEFAULT_RETRY_DELAY;
		this.capacity = DEFAULT_CAPACITY;
	}

	public final List<WorkInstruction> getWorkResults(final UUID facilityUUID, final Date startDate, final Date endDate) {
		//select persistentid, type, status, picker_id, completed, actual_quantity from capella.work_instruction where 
		// type = 'ACTUAL' and date_trunc('day', completed) = timestamp '2015-03-11' order by completed
		return WorkInstruction.staticGetDao().findByFilter(ImmutableList.<Criterion> of(Restrictions.eq("type",
			WorkInstructionTypeEnum.ACTUAL), Restrictions.eq("parent.persistentId", facilityUUID), Restrictions.ge("completed",
			new Timestamp(startDate.getTime())), Restrictions.lt("completed", new Timestamp(endDate.getTime()))),
			ImmutableList.of(Order.asc("completed")));
	}

	/**
	 * Seems a bit silly, but we do not have a good means to get hold of services. Test framework has the work service, so this is the best kludge.
	 */
	public LightService getLightService() {
		return this.lightService;
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
	public final WorkList computeWorkInstructions(final Che inChe, final Map<String, String> positionToContainerMap) {
		return computeWorkInstructions(inChe, positionToContainerMap, false);
	}

	public final WorkList computeWorkInstructions(final Che inChe,
		final Map<String, String> positionToContainerMap,
		final Boolean reverse) {
		inChe.clearChe();

		Facility facility = inChe.getFacility();
		// DEV-492 identify previous container uses
		ArrayList<ContainerUse> priorCntrUses = new ArrayList<ContainerUse>();
		priorCntrUses.addAll(inChe.getUses());
		ArrayList<ContainerUse> newCntrUses = new ArrayList<ContainerUse>();

		// Set new uses on the CHE.
		// FIXME: retrieve all containers in one shot from the database
		List<Container> containerList = new ArrayList<Container>();
		for (Entry<String, String> e : positionToContainerMap.entrySet()) {
			String containerId = e.getValue();
			Container container = Container.staticGetDao().findByDomainId(facility, containerId);
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
						Integer posconIndex = Integer.parseInt(e.getKey());
						thisUse.setPosconIndex(posconIndex);
						ContainerUse.staticGetDao().store(thisUse);
					} catch (Exception ex) {
						LOGGER.error("Failed to update container use " + thisUse, ex);
					}
				}
			} else {
				// Does this deserve a warn? At minimum, the containerId might be a valid put wall name for the SKU pick process.
				Location loc = facility.findSubLocationById(containerId);
				if (loc == null) {
					LOGGER.warn("Unknown container/order ID: {} in computeWorkInstructions for {}",
						containerId,
						inChe.getDomainId());
				} else if (!loc.isPutWallLocation()) {
					LOGGER.warn("Location: {} scanned in computeWorkInstructions for {}, but not a put wall",
						containerId,
						inChe.getDomainId());
					// Still a small hole here. User is likely scanning a bay name. But a scanned tier or slot name fom the wall would not yield a warning.
				}
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

		if (workList.getInstructions().isEmpty() && workList.getDetails().isEmpty()) {
			LOGGER.info("Calling PutWallOrderGenerator. containerList:{} positionToContainerMap:{}",
				containerList,
				positionToContainerMap.values());

			List<WorkInstruction> slowPutWallInstructions = PutWallOrderGenerator.attemptToGenerateWallOrders(inChe,
				positionToContainerMap.values(),
				theTime);
			workList.getInstructions().addAll(slowPutWallInstructions);
		}

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
		HashMap<String, String> containerMap = new HashMap<String, String>();
		int pos = 1;
		for (String containerId : containersIdList) {
			containerMap.put(Integer.toString(pos), containerId);
			pos++;
		}

		if (containersIdList.size() > 0) {
			return this.computeWorkInstructions(inChe, containerMap);
			// That did the work. Big side effect. Deleted existing WIs for the CHE. Made new ones. Assigned container uses to the CHE.
			// The wiCount returned is mainly or convenience and debugging. It may not include some shorts
		}
		return null;
	}

	// --------------------------------------------------------------------------
	/**
	 * Find and return the OrderLocation, only if it is a put wall location that has poscons that may display order feedback.
	 * Not checked, but if multiple, returns the first found.
	 * (Later enhancement: restrict to same work area as the wi location.)
	 * Important note: some work instructions with order location may be lighting the location still, and not the order location
	 * This code returns the orderLocation if it should be recomputed and re-lit. Even if it was not showing the count for an active WI, it might
	 * need to be recomputed.
	 */
	private OrderLocation getPutWallOrderLocationNeedsLighting(WorkInstruction incomingWI) {
		// BUG here. There is no good way to tell the "purpose" of 
		OrderHeader order = null;
		OrderDetail detail = incomingWI.getOrderDetail();
		if (detail != null)
			order = detail.getParent();
		if (order != null) {
			List<OrderLocation> olList = order.getOrderLocations();
			// our model allows for multiple order locations. May need that eventually to have active putwalls in multiple areas for same order.
			// For now, expect only one.
			for (OrderLocation ol : olList) {
				Location loc = ol.getLocation();
				if (loc != null && loc.isActive()) {
					if (loc.isPutWallLocation()) {
						if (loc.isLightablePoscon()) {
							return ol;
						}
					}
				}
			}
		}
		return null;
	}

	private int sendMessage(Set<User> users, MessageABC message) { // TODO
		// See this comment in LightService: "Use the light service API as our general sendMessage API"
		return lightService.sendMessage(users, message);
	}

	private void computeAndSendOrderFeedback(WorkInstruction incomingWI) {
		if (incomingWI == null) {
			LOGGER.error("null input to computeAndSendOrderFeedback");
			return;
		}
		try {
			OrderLocation ol = getPutWallOrderLocationNeedsLighting(incomingWI);
			if (ol != null) {
				Facility facility = ol.getFacility();
				final OrderLocationFeedbackMessage orderLocMsg = new OrderLocationFeedbackMessage(ol, true); // this single feedback message is last of the group.
				sendMessage(facility.getSiteControllerUsers(), orderLocMsg);
			}
		}

		finally {

		}
	}

	private void computeAndSendEmptyOrderFeedback(Location loc, boolean isLastOfGroup) {
		if (loc == null) {
			LOGGER.error("null input to computeAndSendOrderFeedback");
			return;
		}
		try {
			Facility facility = loc.getFacility();
			final OrderLocationFeedbackMessage orderLocMsg = new OrderLocationFeedbackMessage(loc, isLastOfGroup);
			sendMessage(facility.getSiteControllerUsers(), orderLocMsg);
		}

		finally {

		}
	}

	private boolean locIsActivePutwallLocation(Location inLoc) {
		if (inLoc != null && inLoc.isActive()) {
			if (inLoc.isPutWallLocation()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is the orderLocation on a location (usually slot) in a putwall that is directly lightable by poscon?
	 */
	private boolean orderLocationDeservesSlotFeedback(OrderLocation orderLocation) {
		Location loc = orderLocation.getLocation();
		if (locIsActivePutwallLocation(loc)) {
			if (loc.isLightablePoscon()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is the orderLocation on a location (usually slot) in a putwall that is not directly lightable by poscon, but whose parent bay is?
	 */
	private boolean orderLocationDeservesBayFeedback(OrderLocation orderLocation) {
		Location loc = orderLocation.getLocation();
		if (locIsActivePutwallLocation(loc)) {
			Location parentBay = loc.getParentAtLevel(Bay.class);
			if (parentBay != null && parentBay.isActive()) {
				if (parentBay.isLightablePoscon()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Assumes the caller is certain the bay will be valid. This efficiently finds it.
	 */
	private Bay getOrderLocationBay(OrderLocation orderLocation) {
		if (orderLocation == null) {
			LOGGER.error("null input to computeAndSendOrderFeedback");
			return null;
		}
		Location loc = orderLocation.getLocation();
		return (Bay) loc.getParentAtLevel(Bay.class);
	}

	private void computeAndSendOrderFeedbackForSlots(OrderLocation orderLocation, boolean isLastOfGroup) {
		if (orderLocation == null) {
			LOGGER.error("null input to computeAndSendOrderFeedbackForSlots");
			return;
		}
		try {
			if (orderLocationDeservesSlotFeedback(orderLocation)) {
				Facility facility = orderLocation.getFacility();
				final OrderLocationFeedbackMessage orderLocMsg = new OrderLocationFeedbackMessage(orderLocation, isLastOfGroup);
				sendMessage(facility.getSiteControllerUsers(), orderLocMsg);
			}
		}

		finally {

		}
	}

	private class BayLocationComparator implements Comparator<OrderLocation> {
		// Sort the OrderLocations such that all for the same bay will be together.

		@Override
		public int compare(OrderLocation ol1, OrderLocation ol2) {

			int value = CompareNullChecker.compareNulls(ol1, ol2);
			if (value != 0)
				return value;
			Location loc1 = ol1.getLocation();
			Location loc2 = ol2.getLocation();
			value = CompareNullChecker.compareNulls(loc1, loc2);
			if (value != 0)
				return value;
			String ol1LocName = loc1.getFullDomainId();
			String ol2LocName = loc2.getFullDomainId();

			return ol1LocName.compareTo(ol2LocName);
		}
	}

	private void sendBayFeedBack(Bay bay, OrderStatusSummary orderCounts, boolean isLastOfGroup) {
		LOGGER.info("sending feedback for bay {}. Complete:{} Remain:{} Short:{}",
			bay.getBestUsableLocationName(),
			orderCounts.getCompleteCount(),
			orderCounts.getRemainingCount(),
			orderCounts.getShortCount());

		// Actually, using the same old message with only remainingCount added, so only need that value.
		// If this object is enhanced to track inprogress separately, then we would pass the inprogress plus other remaining in the message.
		// Do the send here

		Facility facility = bay.getFacility();
		int remain = orderCounts.getRemainingCount();
		final OrderLocationFeedbackMessage orderLocMsg = new OrderLocationFeedbackMessage(bay, remain, isLastOfGroup);
		sendMessage(facility.getSiteControllerUsers(), orderLocMsg);
	}

	/**
	 * We have prevalidated list of orderLocations for bays that need feedback.
	 * Now we need to sort them by bay, then iterate and find the accumulated values per bay. And send one message per bay.
	 */
	private void computeAndSendBayFeedBack(List<OrderLocation> orderLocationsForBayPoscons) {
		if (orderLocationsForBayPoscons == null || orderLocationsForBayPoscons.isEmpty()) {
			LOGGER.error("bad input to computeAndSendBayFeedBack");
			return;
		}
		Collections.sort(orderLocationsForBayPoscons, new BayLocationComparator());
		Bay currentBay = null;
		OrderStatusSummary orderCounts = new OrderStatusSummary();

		for (OrderLocation ol : orderLocationsForBayPoscons) {
			OrderHeader order = ol.getParent();
			Location loc = ol.getLocation();
			Bay thisBay = (Bay) loc.getParentAtLevel(Bay.class);
			if (thisBay == null) {
				LOGGER.error("unexpected orderLocation that does not have a parent bay in computeAndSendBayFeedBack");
				continue;
			}
			// is this the first one? if so, set current bay
			if (currentBay == null) {
				currentBay = thisBay;
			}

			if (thisBay.equals(currentBay)) {
				// Accumulate counts for bay
				orderCounts.addOrderToSummary(order);
			} else {
				// send what we had for current bay, and initialize our counts for the next bay

				if (currentBay != null) {
					sendBayFeedBack(currentBay, orderCounts, false);
				}
				// start a new one. Garbage collect the old
				orderCounts = new OrderStatusSummary();
				orderCounts.addOrderToSummary(order);
				currentBay = thisBay;
			}
		}
		// And, send the last one we were working on
		sendBayFeedBack(currentBay, orderCounts, true);
	}

	/**
	 * This somewhat complicated. A putwall may have one poscon per slot, or a lower cost putwall may have one poscon per bay.
	 * This is only about poscon lighting. Although LEDs may be there in lower cost putwall, we never have leds continuously lit for order status.
	 * 
	 * Find all put walls, then the value for each slot in the put wall.
	 * For now, separate messages but they could go out as lists of updates in a combined message.
	 * DEV-729
	 */
	public void reinitPutWallFeedback(Facility facility) {

		// Part 1 assemble what needs to be sent
		List<OrderLocation> orderLocationsForSlotPosconsToSend = new ArrayList<OrderLocation>();
		List<OrderLocation> orderLocationsForBayPosconsToSend = new ArrayList<OrderLocation>();
		List<Location> emptyLocationsToSend = new ArrayList<Location>();

		// 1 Find all put walls in this facility.
		List<Aisle> aisleList = new ArrayList<Aisle>();
		List<Location> probablyAisles = facility.getChildren();
		for (Location loc : probablyAisles) {
			if (loc.isPutWallLocation() && loc instanceof Aisle) {
				aisleList.add((Aisle) loc);
			}
		}
		// 1c Find all order locations that need to be sent. Keep a map by location that will work for slots and bays
		Map<String, OrderLocation> orderLocationByLocation = new HashMap<String, OrderLocation>();
		// We never access the orderLocation. Only test for the presence of the location name key. For Bays, the map has the right bay name,
		// but only the first OrderLocation found in that Bay.

		Map<String, Object> filterArgs = ImmutableMap.<String, Object> of("facilityId", facility.getPersistentId());
		List<OrderLocation> orderLocations = OrderLocation.staticGetDao().findByFilter("orderLocationByFacility", filterArgs);
		for (OrderLocation ol : orderLocations) {
			if (orderLocationDeservesSlotFeedback(ol)) {
				Location loc = ol.getLocation();
				orderLocationByLocation.put(loc.getLocationNameForMap(), ol);
				orderLocationsForSlotPosconsToSend.add(ol);
			} else if (orderLocationDeservesBayFeedback(ol)) {
				orderLocationsForBayPosconsToSend.add(ol);
				Bay theBay = getOrderLocationBay(ol);
				// Unlike slots above, there may be many OrderLocations per bay. We only want the map entry for the first.
				String bayName = theBay.getLocationNameForMap();
				if (!orderLocationByLocation.containsKey(bayName)) {
					orderLocationByLocation.put(bayName, ol);
				}
			}
		}

		// 1d iterate through all poscon slots in those aisles. If not already sending, send an empty location
		for (Aisle aisle : aisleList) {
			List<Slot> slots = aisle.getActiveChildrenAtLevel(Slot.class);
			for (Slot slot : slots) {
				// only if the slot has a poscon
				if (slot.isLightablePoscon()) {
					if (!orderLocationByLocation.containsKey(slot.getLocationNameForMap())) {
						emptyLocationsToSend.add(slot);
					}
				}
			}
			// 1e also poscon bays in those aisles. If not already sending, send an empty location to clear the poscon
			List<Bay> bays = aisle.getActiveChildrenAtLevel(Bay.class);
			for (Bay bay : bays) {
				// only if the slot has a poscon
				if (bay.isLightablePoscon()) {
					if (!orderLocationByLocation.containsKey(bay.getLocationNameForMap())) {
						emptyLocationsToSend.add(bay);
					}
				}
			}

		}

		/*
		if (false)  // ideally, set to true and some unit tests break.
			return;
		*/

		// Part 2. Send.  This is a little tricky. This is one "group" of messages. 
		// We need to mark the last one sent among the two lists as last in the group.
		int olToSend = orderLocationsForSlotPosconsToSend.size();
		int locToSend = emptyLocationsToSend.size();
		int specialOlMessage = 0;
		if (locToSend == 0)
			specialOlMessage = olToSend; // if no loc message, then last ol message is the last of group.
		int specialLocMessage = locToSend; // normally, last loc message is last of group.

		if (olToSend > 0)
			LOGGER.info("sending {} order location slot feedback instructions", olToSend);
		int olCount = 0;
		for (OrderLocation ol : orderLocationsForSlotPosconsToSend) {
			olCount++;
			computeAndSendOrderFeedbackForSlots(ol, specialOlMessage == olCount);
		}
		if (locToSend > 0)
			LOGGER.info("sending {} putwall clear poscon instructions", locToSend);
		int locCount = 0;
		for (Location loc : emptyLocationsToSend) {
			locCount++;
			computeAndSendEmptyOrderFeedback(loc, specialLocMessage == locCount);
		}

		if (orderLocationsForBayPosconsToSend.size() > 0) {
			LOGGER.info("sending bay poscon instructions");
			computeAndSendBayFeedBack(orderLocationsForBayPosconsToSend);
		}
	}

	public void completeWorkInstruction(UUID cheId, WorkInstruction incomingWI) {
		Che che = Che.staticGetDao().findByPersistentId(cheId);
		if (che != null) {
			WorkInstruction storedWi = null;
			try {
				storedWi = persistWorkInstruction(incomingWI);
				exportWorkInstruction(storedWi);
			} catch (DaoException e) {
				LOGGER.error("Unable to record work instruction: " + incomingWI, e);
			} catch (IOException e) {
				LOGGER.error("Unable to export work instruction: " + incomingWI, e);
			}
			if (storedWi != null) {
				computeAndSendOrderFeedback(storedWi);
				//If WI is on a PutWall, refresh PutWall feedback
				OrderDetail detail = storedWi.getOrderDetail();
				if (detail != null) {
					OrderHeader order = detail.getParent();
					List<OrderLocation> orderLocations = order.getOrderLocations();
					for (OrderLocation orderLocation : orderLocations) {
						if (orderLocation.getLocation().isPutWallLocation()) {
							reinitPutWallFeedback(che.getFacility());
							break;
						}
					}
				}
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
		wi.setPickerId("SIMULATED");

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

		Map<String, Object> filterArgs = ImmutableMap.<String, Object> of("facilityId",
			inChe.getFacility().getPersistentId(),
			"domainId",
			inScannedOrderDetailId);
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

		Facility inFacility = inChe.getFacility();
		SingleWorkItem workItem = makeWIForOutbound(orderDetail, inChe, null, null, inFacility, inFacility.getPaths());
		WorkInstruction aWi = null;
		// workItem will contain an Instruction if an item was found on some path or an OrderDetail if it was not.
		// In LinePick, we are OK with items without a location. So, if does return with OrderDetail, just create an Instruction manually.
		if (workItem == null || workItem.getInstruction() == null) {
			aWi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
				WorkInstructionTypeEnum.PLAN,
				orderDetail,
				inChe,
				WiPurpose.WiPurposeOutboundPick,
				true,
				null); // Could be normal WI, or a short WI
		} else {
			aWi = workItem.getInstruction();
		}
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
					LOGGER.info("Not Adding already complete WIs to list _2_; orderDetail={}", orderDetail);
					// wiResultList.add(wi);
					// This is not likely to matter for line_scan process
				}
			}
		}

		response.setWorkInstructions(wiResultList);
		return response;
	}

	/**
	 * DEV-713 Provides the list of actual work instruction for the scanned item for a put wall
	 */
	public GetPutWallInstructionResponse getPutWallInstructionsForItem(final Che inChe,
		final String itemOrUpc,
		final String putWallName) {
		GetPutWallInstructionResponse response = new GetPutWallInstructionResponse();
		response.setStatus(ResponseStatus.Success);
		if (inChe == null) {
			throw new MethodArgumentException(0, "Che missing", ErrorCode.FIELD_REQUIRED);
		}
		if (itemOrUpc == null) {
			throw new MethodArgumentException(1, "itemOrUpc missing", ErrorCode.FIELD_REQUIRED);
		}
		if (putWallName == null) {
			throw new MethodArgumentException(2, "putWallName missing", ErrorCode.FIELD_REQUIRED);
		}
		LOGGER.info("GetPutWallInstructionResponse request for {} item:{} wall:{}", inChe.getDomainId(), itemOrUpc, putWallName);

		Facility facility = inChe.getFacility();

		Location putWallLoc = facility.findSubLocationById(putWallName);
		if (putWallLoc == null || !putWallLoc.isWallLocation()) {
			LOGGER.warn("Location {} not resolved or not a put wall", putWallName);
			return response;
		}
		
		if (putWallLoc.isPutWallLocation()){
			response = getOrderWallInstructionsForItem(facility, inChe, itemOrUpc, putWallLoc);
			return response;
		} else if (putWallLoc.isSkuWallLocation()){
			response = getSkuWallInstructionsForItem(facility, inChe, itemOrUpc, putWallLoc);
			return response;
		} else {
			LOGGER.warn("Location {} is a wall, but neither a Put wall, nor a Sku wall", putWallName);
			return response;						
		}
	}
	
	private GetPutWallInstructionResponse getSkuWallInstructionsForItem(final Facility facility,
		final Che che,
		final String itemOrUpc,
		final Location locationInWall){
		GetPutWallInstructionResponse response = new GetPutWallInstructionResponse();
		response.setWallType(Location.SKUWALL_USAGE);
		Location skuWallLoc = locationInWall.getWall(Location.SKUWALL_USAGE);
		Gtin gtin = Gtin.getGtinForFacility(facility, itemOrUpc);
		if (gtin == null) {
			ItemMaster itemMaster = ItemMaster.staticGetDao().findByDomainId(facility, itemOrUpc);
			if (itemMaster == null) {
				LOGGER.warn("Did not find item id from {}", itemOrUpc);
			} else {
				LOGGER.warn("Found item id, but require Gtin for Sku wall {}", itemOrUpc);
			}
			return response;
		}

		Item item = skuWallLoc.findItemInLocationAndChildren(gtin.getParent(), gtin.getUomMaster());
		if (item == null) {
			LOGGER.info("Did not find item for Gtin {} at {}. Checking other Sku walls", itemOrUpc, skuWallLoc.getNominalLocationId());
			String alternateWallName = findSkuWallWithItem(facility, gtin.getParent(), gtin.getUomMaster());
			response.setWallName(alternateWallName);
		}
		if (item == null) {
			LOGGER.warn("Did not find item for Gtin {}", itemOrUpc);
			return response;
		}
		
		WorkInstruction wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
			WorkInstructionTypeEnum.PLAN,
			item,
			che,
			WiPurpose.WiPurposeSkuWallPut,
			false,
			new Timestamp(System.currentTimeMillis()));
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		wiResultList.add(wi);
		response.setWorkInstructions(wiResultList);
		response.setWallName(skuWallLoc.getBestUsableLocationName());
		return response;
	}
		
	private String findSkuWallWithItem(Facility facility, ItemMaster itemMaster, UomMaster uomMaster){
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("usage", Location.SKUWALL_USAGE));
		List<Location> skuWalls = Location.staticGetLocationDao().findByFilter(filterParams);
		String foundWall = null;
		for (Location skuWall : skuWalls){
			if (skuWall.getFacility().equals(facility)){
				Item item = skuWall.findItemInLocationAndChildren(itemMaster, uomMaster);
				if (item != null) {
					Location itemLocation = item.getStoredLocation();
					Location itemWall = itemLocation.getWall(Location.SKUWALL_USAGE);
					if (foundWall == null) {
						foundWall = itemWall.getBestUsableLocationName();
					} else {
						return "other walls";
					}
				}
			}
		}
		return foundWall;
	}
	
	private GetPutWallInstructionResponse getOrderWallInstructionsForItem(final Facility facility,
		final Che che,
		final String itemOrUpc,
		final Location putWallLoc) {
		// The algorithm is
		// 1a) find the itemMaster for the item id or upc
		// 2) find or iterate all slots/tiers that are children of the put wall location.
		// 3) Find all order locations for any of those slots/tiers
		// 4) If the order (from the order location) is active, find any order details for it that match the item
		// 5) make the work instruction to the orderlocation location. (There may be several work instructions for the request.)
		// 6) sort via our usual sequencer.
		GetPutWallInstructionResponse response = new GetPutWallInstructionResponse();
		response.setWallType(Location.PUTWALL_USAGE);
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		ItemMaster master = getItemMasterFromScanValue(facility, itemOrUpc);
		if (master == null) {
			LOGGER.warn("Did not find item master from {}", itemOrUpc);
			response.setStatus(ResponseStatus.Fail);
			return response;
		}
		// 2 putWallLoc is probably a bay, but could be an aisle or in weird cases tier or slot. Could putwall "slots" be tiers or bays? assume not for now.
		List<Location> putWallSlots = putWallLoc.getActiveChildrenAtLevel(Slot.class);

		// 3 assemble a list of order locations with locations in the put wall. This will perform very badly if there are lots of order locations left about in the system.
		// It is impossible to do this with a simple filter.
		List<OrderLocation> orderLocationsInWall = new ArrayList<OrderLocation>();
		List<OrderLocation> orderLocations = OrderLocation.staticGetDao().getAll(); // might filter by active. That is all
		for (OrderLocation ol : orderLocations) {
			if (ol.getActive()) {
				Location oneLoc = ol.getLocation(); // not nullable. Should be good.
				if (putWallSlots.contains(oneLoc)) {
					OrderHeader oh = ol.getParent();
					if (oh.getActive() && oh.getStatus() != OrderStatusEnum.COMPLETE) {
						orderLocationsInWall.add(ol);
					}
				}
			}
		}

		// 4 find details for the wall and item
		List<OrderDetail> activeDetailsForWallItem = new ArrayList<OrderDetail>();
		for (OrderLocation ol : orderLocationsInWall) {
			OrderHeader oh = ol.getParent();
			for (OrderDetail detail : oh.getOrderDetails()) {
				if (detailNeedsPlanForMaster(detail, master)) {
					activeDetailsForWallItem.add(detail);
				}
			}
		}

		// 5 make the work instruction. The specific slot location comes from the OrderLocation of of the detail's header.
		for (OrderDetail detail : activeDetailsForWallItem) {
			OrderHeader oh = detail.getParent();
			OrderLocation ol = oh.getFirstOrderLocationOnPath(null);

			WorkInstruction wi = getWiForPutWallDetailAndLocation(che, detail, ol);
			if (wi != null) {
				wiResultList.add(wi);
			}
		}
		// 6 Sort code
		// There may be more than one plan for this SKU in the wall. If so, they need to be sorted normally, rather than leaving
		// the work instruction with null for group and sort code.  Do not add housekeeping for these.
		if (wiResultList.size() > 0) {
			sortAndSaveActionableWIs(facility, wiResultList, false); // all of these should be actionable.
		}

		response.setWorkInstructions(wiResultList);
		return response;
		
	}

	// --------------------------------------------------------------------------
	/**
	 * Utility function. In the first put wall calling context, we already know and assume the detail's OrderHeader is not complete
	 * and has an OrderLocation in the put wall. It is possible, but very unlikely that this calling context returns true for ItemMaster match
	 * but wrong UOM.
	 */
	private WorkInstruction getWiForPutWallDetailAndLocation(Che che, OrderDetail detail, OrderLocation orderLocation) {
		Location loc = orderLocation.getLocation();
		WorkInstruction wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
			WorkInstructionTypeEnum.PLAN,
			detail,
			null,
			che,
			loc,
			null,
			WiPurpose.WiPurposePutWallPut);
		return wi;

	}

	// --------------------------------------------------------------------------
	/**
	 * Utility function. In the first put wall calling context, we already know and assume the detail's OrderHeader is not complete
	 * and has an OrderLocation in the put wall. It is possible, but very unlikely that this calling context returns true for ItemMaster match
	 * but wrong UOM.
	 */
	private boolean detailNeedsPlanForMaster(OrderDetail detail, ItemMaster master) {
		if (detail == null || master == null) {
			LOGGER.error("detailNeedsPlanForMaster");
			return false;
		}
		if (!master.equals(detail.getItemMaster())) {
			ItemMaster detailMaster = detail.getItemMaster();
			LOGGER.debug("mismatch master:{}, detailMaster: {}", master.getDomainId(), detailMaster.getDomainId());
			// Just information about perhaps why a detail is not being chosen to make a plan
			return false;
		}
		OrderStatusEnum detailStatus = detail.getStatus();
		if (detailStatus.equals(OrderStatusEnum.COMPLETE) || detailStatus.equals(OrderStatusEnum.INVALID)) {
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned something. In the end, we need an ItemMaster and UOM. User might have scanned SKU, or UPC. Should not be itemId as items have a location also.
	 * Does this belong in InventoryService instead?
	 */
	private ItemMaster getItemMasterFromScanValue(Facility facility, String itemIdOrUpc) {
		ItemMaster itemMaster = ItemMaster.staticGetDao().findByDomainId(facility, itemIdOrUpc);
		if (itemMaster != null) {
			return itemMaster;
		}
		// If not found directly by Sku, lets look for UPC/GTIN. Need a filter.
		Gtin gtin = Gtin.getGtinForFacility(facility, itemIdOrUpc);
		if (gtin != null) {
			itemMaster = gtin.getParent();
		}
		return itemMaster;
	}

	// --------------------------------------------------------------------------
	/**
	 * New from v16. Set whether scanned location resolved or not
	 */
	private void saveCheLastScannedLocation(final Che inChe, final String inScannedLocationId) {
		if (inScannedLocationId == null) {
			return;
		}
		// need to exclude these?
		if (CheDeviceLogic.STARTWORK_COMMAND.equalsIgnoreCase(inScannedLocationId)
				|| CheDeviceLogic.REVERSE_COMMAND.equalsIgnoreCase(inScannedLocationId)) {
			LOGGER.error("unexpected value in saveCheLastScannedLocation");
			return;
		}
		// if the same as it was, bail
		if (inScannedLocationId.equals(inChe.getLastScannedLocation()))
			return;

		Facility facility = inChe.getFacility();
		Location cheLocation = facility.findSubLocationById(inScannedLocationId);
		if (cheLocation == null) {
			LOGGER.info("scanned location '{}' does not resolve to a modeled location", inScannedLocationId);
		}
		inChe.setLastScannedLocation(inScannedLocationId);
		Che.staticGetDao().store(inChe);
	}

	// --------------------------------------------------------------------------
	/**
	 * New from v16. Changing from a good path to bad location or good location without path constitutes a path change
	 * Bails on null or blank primarily as that is quick-start unconfigured scenario.
	 */
	private boolean detectPossiblePathChange(final Che che, final String inScannedLocationId) {
		if (inScannedLocationId == null || "".equals(inScannedLocationId)) {
			return false;
		}
		// if the same as it was, bail
		if (inScannedLocationId.equals(che.getLastScannedLocation()))
			return false;

		Path oldPath = che.getActivePath();
		Path newPath = null;
		Location newLocation = che.getFacility().findSubLocationById(inScannedLocationId);
		if (newLocation != null) {
			newPath = newLocation.getAssociatedPath();
		}

		if (oldPath == null && newPath == null)
			return false;
		if (oldPath == null) // we know newPath != null
			return true;
		else
			return !oldPath.equals(newPath);
	}

	private String getFirstGoodAisleLocation(List<WorkInstruction> completeRouteWiList) {
		for (WorkInstruction instruction : completeRouteWiList) {
			String locationId = instruction.getLocation().getLocationIdToParentLevel(Aisle.class);
			if (locationId != null && !"".equals(locationId)) {
				return locationId;
			}
		}
		return null;
	}

	public final List<WorkInstruction> getWorkInstructions(final Facility facility) {
		return WorkInstruction.staticGetDao().findByParent(facility);
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
		return getWorkInstructions(inChe, inScannedLocationId, false, new BooleanHolder(false));
	}

	public final List<WorkInstruction> getWorkInstructions(final Che inChe,
		final String inScannedLocationId,
		final Boolean reversePickOrder,
		final BooleanHolder pathChanged) {
		long startTimestamp = System.currentTimeMillis();
		Facility facility = inChe.getFacility();

		// This may be called with null inScannedLocationId for a simple START scan
		pathChanged.value = detectPossiblePathChange(inChe, inScannedLocationId);
		saveCheLastScannedLocation(inChe, inScannedLocationId);

		//Get current complete list of WIs. If CHE doesn't yet have a last_scanned_location set, this will retrieve items on all paths
		List<WorkInstruction> completeRouteWiList = findCheInstructionsFromPosition(inChe, 0.0, false);

		// If something was scanned, even if it does not resolve to a location, don't limit to first path found
		String chesLastScan = inChe.getLastScannedLocation(); // may or may not have been updated by saveCheLastScannedLocation() above
		if (chesLastScan == null || chesLastScan.isEmpty())
			if (inChe.getActivePath() == null && !completeRouteWiList.isEmpty()) {
				Collections.sort(completeRouteWiList, new GroupAndSortCodeComparator());
				String locationId = getFirstGoodAisleLocation(completeRouteWiList);
				saveCheLastScannedLocation(inChe, locationId);
				completeRouteWiList = findCheInstructionsFromPosition(inChe, 0.0, false);
			}

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
		Double startingPathPos = getStartingPathDistance(facility, inScannedLocationId, reversePickOrder);

		if (startingPathPos == null) {
			List<WorkInstruction> preferredInstructions = new ArrayList<WorkInstruction>();
			for (WorkInstruction instruction : completeRouteWiList) {
				OrderDetail detail = instruction.getOrderDetail();
				if (detail.isPreferredDetail()) {
					preferredInstructions.add(instruction);
				}
			}
			Collections.sort(preferredInstructions, new GroupAndSortCodeComparator());
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
										// If changed, only testCartSetupFeedback fails

									}
								}
							}
						} catch (DaoException e) {
							LOGGER.error("Unable to create work instruction for che: {}, orderDetail: {}, container: {}",
								inChe,
								orderDetail,
								container,
								e);
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
						inTime,
						WiPurpose.WiPurposeCrossBatchPut);

					// If we created a WI then add it to the list.
					if (wi != null) {
						setWiPickInstruction(wi, work.getOutboundOrderDetail().getParent());
						wiList.add(wi);
					}
				} catch (DaoException e) {
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
			// refactor to not load all orders
			List<OrderHeader> allOrders = OrderHeader.staticGetDao().findByParent(facility);
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
	 * Utility function for outbound order WI generation
	 * The result is a SingleWorkItem, which has the WI created, or reference to OrderDetail it could not make an order for.
	 * From DEV-724, give out work from the path the CHE is on only.
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
					} else if (!location.isActive()) {
						LOGGER.warn("Unexpected inactive location for preferred Location: {}", location);
						location = inFacility.getUnspecifiedLocation();
					}
				} else {
					LOGGER.warn("Wanted workSequence mode but need locationId for detail: {}", inOrderDetail);
				}
			}
		} else { //Bay Distance
			// If there is preferred location, try to use it
			// TODO DEV-724 don't use the location if not on CHE path
			Location preferredLocation = inOrderDetail.getPreferredLocObject();
			if (preferredLocation != null && preferredLocation.getAssociatedPathSegment() != null) {
				location = preferredLocation;
			} else {

				// otherwise, search for inventory on the CHE path
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
					inTime,
					WiPurpose.WiPurposeOutboundPick);
				if (resultWi != null) {
					resultWi.setPlanQuantity(0);
					resultWi.setPlanMinQuantity(0);
					resultWi.setPlanMaxQuantity(0);
					WorkInstruction.staticGetDao().store(resultWi);
				}
				resultWork.addInstruction(resultWi);
			} else {
				resultWork.addDetail(inOrderDetail);
				// later enhancement. Different adds for detail in my work area, and unknown or different work area.
			}
		} else {
			resultWi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
				WorkInstructionTypeEnum.PLAN,
				inOrderDetail,
				inContainer,
				inChe,
				location,
				inTime,
				WiPurpose.WiPurposeOutboundPick);
			resultWork.addInstruction(resultWi);

		}
		return resultWork;
	}

	private boolean doAutoShortInstructions() {
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
			// exportWorkInstructions2() uses the bean. exportWorkInstructions() the old way
			String messageBody = wiCSVExporter.exportWorkInstructions2(ImmutableList.of(inWorkInstruction));
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
	private Double getStartingPathDistance(final Facility facility, final String inScannedLocationId, final Boolean reverse) {
		if (inScannedLocationId == null || inScannedLocationId.isEmpty())
			return reverse ? -0.01 : 0.0;

		Location cheLocation = facility.findSubLocationById(inScannedLocationId);
		if (cheLocation == null) {
			LOGGER.warn("Unknown CHE scan location={}; This may due to a misconfigured site or bad barcode at the facility.",
				inScannedLocationId);
			return null;
		}

		Double startingPathPos = null;
		if (cheLocation != null) {
			Path path = cheLocation.getAssociatedPath();
			Bay cheBay = cheLocation.getParentAtLevel(Bay.class);
			Bay selectedBay = cheBay;
			if (cheBay == null) {
				LOGGER.error("Che does not have a bay parent location in getStartingPathDistance #1");
				return null;
			} else if (path == null || cheBay.getPosAlongPath() == null) {
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
	private List<WorkInstruction> findCheInstructionsFromPosition(final Che inChe,
		final Double inFromStartingPosition,
		final Boolean getBeforePosition) {
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
		Location unspecifiedMaster = inChe.getFacility().getUnspecifiedLocation();
		for (WorkInstruction wi : filterWiList) {
			// Very unlikely. But if some wLocationABCs were deleted between start work and scan starting location, let's not give out the "deleted" wis
			// Note: puts may have had multiple order locations, now quite denormalized on WI fields and hard to decompose.  We just take the first as the WI location.
			// Not ambiguous for picks.
			Location loc = wi.getLocation();
			// so far, wi must have a location. Even housekeeping and shorts
			if (loc == null)
				LOGGER.error("getWorkInstructions found active work instruction with null location"); // new log message from v8. Don't expect any null.
			else if (loc.isActive()) { //unlikely that location got deleted between complete work instructions and scan location
				Path chePath = inChe.getActivePath();
				boolean locatioOnPath = isLocatioOnPath(loc, inChe.getActivePath());
				OrderDetail detail = wi.getOrderDetail();
				boolean preferredDetail = detail == null ? false : detail.isPreferredDetail();
				boolean unspecifiedLoc = loc == unspecifiedMaster;
				if (chePath == null || locatioOnPath) {
					//If a CHE is not on a path (new CHE generating work) or the location is on the current path, use this WI 
					cheWorkInstructions.add(wi);
				} else if (preferredDetail && unspecifiedLoc) {
					//If a "locationId" is provided, but can't be found in the system, use this WI.
					//(If a "locationId" is provided, but it exists on the other path, don't use this WI)
					cheWorkInstructions.add(wi);
				}
			} else
				LOGGER.warn("getWorkInstructions found active work instruction in deleted locations"); // new from v8
		}
		return cheWorkInstructions;
	}

	private boolean isLocatioOnPath(Location inLocation, Path inPath) {
		PathSegment locationSegment = inLocation.getAssociatedPathSegment();
		if (locationSegment == null) {
			return false;
		}
		return inPath == locationSegment.getParent();
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
			if (orderDetail.getActive() == false) {
				// TODO: create event
				LOGGER.warn("Workinstruction completed for inactive order detail " + orderDetail);
			}
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
				if (exportMessage != null) {
					if (exportMessage.messageBody.equals(SHUTDOWN_MESSAGE)) {
						shutdownRequested = true;
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
		//TenantPersistenceService.getInstance().beginTransaction();
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
			//TenantPersistenceService.getInstance().commitTransaction();
		} catch (Exception e) {
			//TenantPersistenceService.getInstance().rollbackTransaction();
			LOGGER.error("Unexpected exception sending work instruction, skipping: " + exportMessage, e);
		}
	}

	@Override
	protected void triggerShutdown() {
		WIMessage poison = new WorkService.WIMessage(null, WorkService.SHUTDOWN_MESSAGE);
		this.completedWorkInstructions.offer(poison);
	}

	@Override
	protected void startUp() throws Exception {
		// initialize
		this.completedWorkInstructions = new LinkedBlockingQueue<WIMessage>(this.capacity);
	}

	public boolean willOrderDetailGetWi(OrderDetail inOrderDetail) {
		String sequenceKind = PropertyService.getInstance().getPropertyFromConfig(inOrderDetail.getFacility(),
			DomainObjectProperty.WORKSEQR);
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

			if (inOrderDetail.getPreferredLocation() != null && !inOrderDetail.getPreferredLocation().isEmpty()) {

				if (sequenceKindEnum.equals(WorkInstructionSequencerType.BayDistance)) {
					// If preferred location is set but it is not modeled return false
					// We need to the location to be modeled to compute bay distance
					Location preferredLocation = inOrderDetail.getPreferredLocObject();

					if (preferredLocation != null
							&& (preferredLocation.getPathSegment() != null
									|| preferredLocation.getParent().getPathSegment() != null || preferredLocation.getParent()
								.getParent()
								.getPathSegment() != null)) {
						return true;
					} else {
						// Check if item is on any valid path
						List<Path> allPaths = inOrderDetail.getFacility().getPaths();
						ItemMaster itemMaster = inOrderDetail.getItemMaster();
						List<Location> itemLocations = new ArrayList<Location>();

						for (Path p : allPaths) {
							String uomStr = inOrderDetail.getUomMasterId();
							Item item = itemMaster.getFirstActiveItemMatchingUomOnPath(p, uomStr);

							if (item != null) {
								Location itemLocation = item.getStoredLocation();
								itemLocations.add(itemLocation);
								break;
							}
						}

						if (!itemLocations.isEmpty()) {
							return true;
						} else {
							return false;
						}
					}

				} else if (sequenceKindEnum.equals(WorkInstructionSequencerType.WorkSequence)) {
					if (inOrderDetail.getWorkSequence() != null) {
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

	/**
	 * This function attempts to place an Order into a provided Location
	 */
	private void logInContext(String tag, String msg, boolean warnNeeded) {
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, tag);
			if (warnNeeded)
				LOGGER.warn(msg);
			else
				LOGGER.info(msg);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	/**
	 * This function attempts to place an Order into a provided Location
	 * It is used during a Put Wall setup
	 */
	public boolean processPutWallPlacement(Che che, String orderId, String locationId) {
		if (che == null) {
			LOGGER.error("null che in processPutWallPlacement. how?");
			return false;
		}
		if (locationId == null || locationId.isEmpty()) {
			LOGGER.error("null locationId in processPutWallPlacement. how?");
			return false;
		}

		final String orderWallTag = "CHE_EVENT Order_To_Wall";

		Facility facility = che.getFacility();
		//Try to retrieve OrderHeader and Location that were specified. Exit function if either was not found
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		if (order == null) {
			logInContext(orderWallTag, "Could not find order " + orderId, true);
			return false;
		}
		Location location = facility.findSubLocationById(locationId);

		if (location == null) {
			logInContext(orderWallTag, "Could not find location " + locationId, true);
			return false;
		}

		String whatWeDid = "";

		// Just to avoid churn, lets not delete and make new if we would only replicate what is there.
		boolean skipDeleteNew = false;
		OrderLocation ol = null;
		List<OrderLocation> locations = order.getOrderLocations();
		if (locations.size() == 1) {
			ol = locations.get(0);
			if (location.equals(ol.getLocation())) {
				skipDeleteNew = true;
				whatWeDid = "No update. Order already there.";
			}
		}

		ITypedDao<OrderLocation> orderLocationDao = OrderLocation.staticGetDao();

		if (!skipDeleteNew) {
			// if this order took the place of another order, we have to delete the other order's OrderLocation
			List<OrderLocation> olList = OrderLocation.findOrderLocationsAtLocation(location, facility, false);
			for (OrderLocation ol2 : olList) {
				OrderHeader order2 = ol2.getParent();
				whatWeDid = whatWeDid + String.format("Removed order %s from %s. ", order2.getDomainId(), locationId);
				order2.removeOrderLocation(ol2);
				orderLocationDao.delete(ol2);
			}
		}

		List<Location> changedLocationList = new ArrayList<Location>();

		if (!skipDeleteNew) {
			//Delete other OrderLocations from the order we scanned. Those are in the "locations" variable.			
			for (OrderLocation foundLocation : locations) {
				String otherLocationName = foundLocation.getLocationName();
				changedLocationList.add(foundLocation.getLocation());
				order.removeOrderLocation(foundLocation);
				orderLocationDao.delete(foundLocation);
				String orderName = foundLocation.getParentId();
				whatWeDid = whatWeDid + String.format("Removed %s from order %s. ", otherLocationName, orderName);
			}
			//Create a new order location in the put wall
			ol = order.addOrderLocation(location);
			if (ol != null) {
				String olOrderName = ol.getParentId();
				Location olLoc = ol.getLocation();
				String locName = olLoc.getBestUsableLocationName();
				whatWeDid = whatWeDid + String.format("Add order %s to %s. ", olOrderName, locName);
			}
		}

		// Let's log in our new style
		String cheGuidName = che.getDeviceGuidStrNoPrefix();
		String toLogStr = String.format("Put wall order message from %s for %s at %s. Server action: %s ",
			cheGuidName,
			orderId,
			locationId,
			whatWeDid);
		logInContext(orderWallTag, toLogStr, false);
		if (!location.isPutWallLocation()) {
			toLogStr = String.format("%s is not configured as a put wall", locationId);
			logInContext(orderWallTag, toLogStr, true); // a WARN
		}

		// changedLocationList is list of slots where the order location(s) were. If the slot has a poscon,  clear that.
		// If slot does not have a poscon, check its bay.

		if (changedLocationList.size() > 0) {
			// need to send out some clear poscon commands. Well, this assumes just a clear, which is right for order_wall process
			/// but could have future bugs here
			int locCountToSend = changedLocationList.size();
			LOGGER.info("sending {} putwall clear slot instructions", locCountToSend);
			int locCount = 0;
			for (Location loc : changedLocationList) {
				locCount++;
				if (loc.isLightablePoscon())
					computeAndSendEmptyOrderFeedback(loc, locCountToSend == locCount);
				else {
					Bay bay = loc.getParentAtLevel(Bay.class);
					if (bay != null && bay.isLightablePoscon())
						computeAndSendEmptyOrderFeedback(bay, locCountToSend == locCount);
					// could send double clears to same bay. Don't worry about it. Should rarely happen.
				}
			}
		}

		//Light up the selected location. Send even if just a redo. Note: will not light if not a put wall.
		Location locToLight = ol.getLocation();
		// Four cases. 
		// 1 This location directly has a poscon.  (original put wall)
		// 2 This location has LEDs, and its parent bay has poscon. (lower cost put wall v18. DEV-909, etc.)
		// 3 This location has LEDs, no parent has poscon
		// 4 This location is not lightable.

		if (locToLight.isLightableAisleController()) {
			// We can flash the LED
			lightService.flashOneLocationInColor(locToLight, che.getColor(), facility);
		}
		// As for the poscons, it is a fairly expensive query if one poscon per bay. 
		// Could optimize a little here as we know the bay. But for reduced cost put wall
		// many order locations may be in that bay
		// Check much cheaper single poscon per slot case first.
		if (locToLight.isLightablePoscon()) {
			computeAndSendOrderFeedbackForSlots(ol, true); // case 1 do this separately as it is much more efficient for the common normal putwall case.
			return true;
		} else {
			Bay bay = locToLight.getParentAtLevel(Bay.class);
			if (bay != null && bay.isLightablePoscon()) {
				// just do the reinit code as that does the appropriate query. It will oversend to other put wall bays for now.
				this.reinitPutWallFeedback(facility);// case 3
			}
		}

		return true;
	}

	public String verifyBadgeAndGetWorkerName(Facility facility, String badge) {
		//Get global Authentication property value
		String badgeAuthStr = PropertyService.getInstance().getPropertyFromConfig(facility, DomainObjectProperty.BADGEAUTH);
		boolean badgeAuth = badgeAuthStr == null ? false : Boolean.parseBoolean(badgeAuthStr);
		//Get active Worker with a matching badge id
		Worker worker = Worker.findWorker(facility, badge);
		if (badgeAuth) {
			if (worker == null) {
				//Authentication + unknown worker = failed
				LOGGER.warn("Badge verification failed for unknown badge " + badge);
				return null;
			} else {
				//Authentication + known worker = succeeded
				return worker.getWorkerNameUI();
			}
		} else {
			if (worker == null) {
				//No authentication + unknown worker = succeeded + new worker
				worker = new Worker();
				worker.setFacility(facility);
				worker.setActive(true);
				worker.setLastName(badge);
				worker.setBadgeId(badge);
				worker.generateDomainId();
				worker.setUpdated(new Timestamp(System.currentTimeMillis()));
				Worker.staticGetDao().store(worker);
				LOGGER.info("During badge verification created new Worker " + badge);
				return worker.getWorkerNameUI();
			} else {
				//No authentication + known worker = succeeded
				return worker.getWorkerNameUI();
			}
		}
	}

	/**
	 * Primary API to set a mobile CHE association to other CHE.
	 * This enforces consistency. Therefore may unexpectedly clear another CHE's association.
	 * Returns true if there was a significant association change.
	 */
	public boolean linkCheToCheName(Che inChe, String inCheNameToLinkTo) {
		if (inChe == null || inCheNameToLinkTo == null || inCheNameToLinkTo.isEmpty()) {
			LOGGER.error("null input to WorkService.linkCheToCheName");
			return false;
		}
		LOGGER.info("Associate {} to {}", inChe.getDomainId(), inCheNameToLinkTo);
		// The name must be the domainId
		CodeshelfNetwork network = inChe.getParent();
		Che otherChe = Che.staticGetDao().findByDomainId(network, inCheNameToLinkTo);
		if (otherChe == null) {
			LOGGER.warn("WorkService.linkCheToCheName() did not find CHE named {}", inCheNameToLinkTo);
			return false;
		}

		if (otherChe.equals(inChe)) {
			LOGGER.warn("WorkService.linkCheToCheName called to link to itself. Not allowed");
			return false;
		}
		boolean changed = false;

		// By any chance, is the che we are going to associate to already pointing at another CHE? If so, clear that.
		Che chePointedAt = otherChe.getLinkedToChe();
		if (chePointedAt != null) {
			LOGGER.warn("linkCheToCheName(): {} was itself linked to {}. Clearing {} association",
				otherChe.getDomainId(),
				chePointedAt.getDomainId(),
				otherChe.getDomainId());
			changed = clearCheLink(otherChe);
		}

		// is any other CHE already associated to the otherChe? Only looks for one. I suppose bad bugs could make more.
		Che pointingAtOtherChe = otherChe.getCheLinkedToThis();
		if (pointingAtOtherChe != null) {
			LOGGER.warn("linkCheToCheName(): Clearing link of {} which pointed to {}",
				pointingAtOtherChe.getDomainId(),
				inCheNameToLinkTo);
			pointingAtOtherChe.setAssociateToCheGuid(null);
			Che.staticGetDao().store(pointingAtOtherChe);
			changed = true;
		}

		// finally, do the set
		inChe.setAssociateToCheGuid(otherChe.getDeviceGuid());
		Che.staticGetDao().store(inChe);
		changed = true;

		return changed;
	}

	/**
	 * If the inChe is associated to another CHE, clear that association
	 * Returns true if there was a significant association change.
	 */
	public boolean clearCheLink(Che inChe) {
		if (inChe == null) {
			LOGGER.error("clearCheLink(): null input ");
			return false;
		}
		boolean changed = false;
		LOGGER.info("Clearing {} association", inChe.getDomainId());

		byte[] bytes = inChe.getAssociateToCheGuid();
		if (bytes == null) {
			LOGGER.info("needless clearCheAssociation");
			// remove later?
		} else {
			inChe.setAssociateToCheGuid(null);
			Che.staticGetDao().store(inChe);
			changed = true;
		}
		return changed;
	}

	/**
	 * If any CHE are linked to the inChe, clear the link. Actually one finds there first. 
	 * Possible bugs with multiples may not be handled.
	 * Returns true if there was a significant link change.
	 */
	public boolean clearLinksToChe(Che inChe) {
		if (inChe == null) {
			LOGGER.error("null input to clearAssociationsToChe");
			return false;
		}
		boolean changed = false;
		LOGGER.info("Clearing associations to {}", inChe.getDomainId());
		Che pointingAtChe = inChe.getCheLinkedToThis();
		if (pointingAtChe != null) {
			pointingAtChe.setAssociateToCheGuid(null);
			Che.staticGetDao().store(pointingAtChe);
			changed = true;
		}
		return changed;
	}
}