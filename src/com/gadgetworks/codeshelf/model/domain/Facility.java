/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

import lombok.Getter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.annotation.Transactional;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.BayDistanceWorkInstructionSequencer;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencer;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Facility
 *
 * The basic unit that holds all of the locations and equipment for a single facility in an organization.
 *
 * @author jeffw
 */

@Entity
@DiscriminatorValue("FACILITY")
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Facility extends SubLocationABC<Facility> {

	@Inject
	public static ITypedDao<Facility>	DAO;

	@Singleton
	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		@Inject
		public FacilityDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}
	}

	private static final Logger				LOGGER				= LoggerFactory.getLogger(Facility.class);

	private static final Integer			MAX_WI_DESC_BYTES	= 80;

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	private Organization					parentOrganization;

	//	@Column(nullable = false)
	//	@ManyToOne(optional = false)
	//	private SubLocationABC					parent;

	@OneToMany(mappedBy = "parent")
	@Getter
	private List<Aisle>						aisles				= new ArrayList<Aisle>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, Container>			containers			= new HashMap<String, Container>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
	@MapKey(name = "domainId")
	private Map<String, ContainerKind>		containerKinds		= new HashMap<String, ContainerKind>();

	@OneToMany(mappedBy = "parent", targetEntity = EdiServiceABC.class)
	@Getter
	private List<IEdiService>				ediServices			= new ArrayList<IEdiService>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, ItemMaster>			itemMasters			= new HashMap<String, ItemMaster>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, CodeshelfNetwork>	networks			= new HashMap<String, CodeshelfNetwork>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, OrderGroup>			orderGroups			= new HashMap<String, OrderGroup>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, OrderHeader>		orderHeaders		= new HashMap<String, OrderHeader>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, Path>				paths				= new HashMap<String, Path>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, UomMaster>			uomMasters			= new HashMap<String, UomMaster>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, LocationAlias>		locationAliases		= new HashMap<String, LocationAlias>();

	public Facility() {
		super(Point.getZeroPoint(), Point.getZeroPoint());
	}

	public Facility(final Point inAnchorPoint) {
		super(inAnchorPoint, Point.getZeroPoint());
	}

	public final String getDefaultDomainIdPrefix() {
		return "F";
	}

	public final ITypedDao<Facility> getDao() {
		return DAO;
	}

	@Override
	public final String getFullDomainId() {
		return getParentOrganization().getDomainId() + "." + getDomainId();
	}

	public final void setParent(Organization inParentOrganization) {
		setParentOrganization(inParentOrganization);
		//parent = inParentOrganization;
		setParent((Facility) null);
	}

	public final String getParentOrganizationID() {
		String result = "";
		if (getParent() != null) {
			result = getParent().getDomainId();
		}
		return result;
	}

	public final void setParentOrganization(final Organization inParentOrganization) {
		parentOrganization = inParentOrganization;
		// for now, facility parent is null, not self.
		// setParent(this);
		setParent((Facility) null);

	}

	public final void setFacilityId(String inFacilityId) {
		setDomainId(inFacilityId);
	}

	public final void addAisle(Aisle inAisle) {
		aisles.add(inAisle);
	}

	public final void removeAisle(Aisle inAisle) {
		aisles.remove(inAisle);
	}

	public final void addPath(Path inPath) {
		paths.put(inPath.getDomainId(), inPath);
	}

	public final Path getPath(String inPathId) {
		return paths.get(inPathId);
	}

	public final List<Path> getPaths() {
		return new ArrayList<Path>(paths.values());
	}

	public final void removePath(String inPathId) {
		paths.remove(inPathId);
	}

	public final void addContainer(Container inContainer) {
		containers.put(inContainer.getDomainId(), inContainer);
	}

	public final Container getContainer(String inContainerId) {
		return containers.get(inContainerId);
	}

	public final List<Container> getContainers() {
		return new ArrayList<Container>(containers.values());
	}

	public final void removeContainer(String inContainerId) {
		containers.remove(inContainerId);
	}

	public final void addContainerKind(ContainerKind inContainerKind) {
		containerKinds.put(inContainerKind.getDomainId(), inContainerKind);
	}

	public final ContainerKind getContainerKind(String inContainerKindId) {
		return containerKinds.get(inContainerKindId);
	}

	public final void removeContainerKind(String inContainerKindId) {
		containerKinds.remove(inContainerKindId);
	}

	public final void addEdiService(IEdiService inEdiService) {
		ediServices.add(inEdiService);
	}

	public final void removeEdiService(IEdiService inEdiService) {
		ediServices.remove(inEdiService);
	}

	public final void addItemMaster(ItemMaster inItemMaster) {
		itemMasters.put(inItemMaster.getDomainId(), inItemMaster);
	}

	public final ItemMaster getItemMaster(String inItemMasterId) {
		return itemMasters.get(inItemMasterId);
	}

	public final void removeItemMaster(String inItemMasterId) {
		itemMasters.remove(inItemMasterId);
	}

	public final List<ItemMaster> getItemMasters() {
		return new ArrayList<ItemMaster>(itemMasters.values());
	}

	public final void addOrderGroup(OrderGroup inOrderGroup) {
		orderGroups.put(inOrderGroup.getDomainId(), inOrderGroup);
	}

	public final OrderGroup getOrderGroup(String inOrderGroupId) {
		return orderGroups.get(inOrderGroupId);
	}

	public final void removeOrderGroup(String inOrderGroupId) {
		orderGroups.remove(inOrderGroupId);
	}

	public final List<OrderGroup> getOrderGroups() {
		return new ArrayList<OrderGroup>(orderGroups.values());
	}

	public final void addOrderHeader(OrderHeader inOrderHeader) {
		orderHeaders.put(inOrderHeader.getDomainId(), inOrderHeader);
	}

	public final OrderHeader getOrderHeader(String inOrderHeaderId) {
		return orderHeaders.get(inOrderHeaderId);
	}

	public final void removeOrderHeader(String inOrderHeaderId) {
		orderHeaders.remove(inOrderHeaderId);
	}

	public final List<OrderHeader> getOrderHeaders() {
		return new ArrayList<OrderHeader>(orderHeaders.values());
	}

	public final void addUomMaster(UomMaster inUomMaster) {
		uomMasters.put(inUomMaster.getDomainId(), inUomMaster);
	}

	public final UomMaster getUomMaster(String inUomMasterId) {
		return uomMasters.get(inUomMasterId);
	}

	public final void removeUomMaster(String inUomMasterId) {
		uomMasters.remove(inUomMasterId);
	}

	public final void addNetwork(CodeshelfNetwork inNetwork) {
		networks.put(inNetwork.getDomainId(), inNetwork);
	}

	public final CodeshelfNetwork getNetwork(String inNetworkId) {
		return networks.get(inNetworkId);
	}

	public final void removeNetwork(String inNetworkId) {
		networks.remove(inNetworkId);
	}

	public final List<CodeshelfNetwork> getNetworks() {
		return new ArrayList<CodeshelfNetwork>(networks.values());
	}

	public final void addLocationAlias(LocationAlias inLocationAlias) {
		locationAliases.put(inLocationAlias.getDomainId(), inLocationAlias);
	}

	public final LocationAlias getLocationAlias(String inLocationAliasId) {
		return locationAliases.get(inLocationAliasId);
	}

	public final void removeLocationAlias(String inLocationAliasId) {
		locationAliases.remove(inLocationAliasId);
	}

	public final List<LocationAlias> getLocationAliases() {
		return new ArrayList<LocationAlias>(locationAliases.values());
	}

	public final Point getAbsoluteAnchorPoint() {
		return Point.getZeroPoint();
	}

	public final Double getAbsolutePosX() {
		return 0.0;
	}

	public final Double getAbsolutePosY() {
		return 0.0;
	}

	public final Double getAbsolutePosZ() {
		return 0.0;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a new aisle with prototype bays.
	 * @param inPosX
	 * @param inPosY
	 * @param inProtoBayXDim
	 * @param inProtoBayYDim
	 * @param inProtoBayZDim
	 * @param inBaysHigh
	 * @param inBaysLong
	 */
	@Transactional
	public final void createAisle(final String inAisleId,
		final Point inAnchorPoint,
		final Point inProtoBayPoint,
		final Integer inBaysHigh,
		final Integer inBaysLong,
		final String inLedControllerId,
		final Boolean inRunInXDir,
		final Boolean inLeftHandBay) {

		CodeshelfNetwork network = networks.get(CodeshelfNetwork.DEFAULT_NETWORK_ID);
		if (network != null) {
			LedController ledController = network.getLedController("LED1");
			if (ledController == null) {
				ledController = network.findOrCreateLedController(inLedControllerId, new NetGuid(inLedControllerId));
			}
			// Create the aisle if it doesn't already exist.
			Aisle aisle = Aisle.DAO.findByDomainId(this, inAisleId);
			if (aisle == null) {
				Point pickFaceEndPoint = computePickFaceEndPoint(inAnchorPoint, inProtoBayPoint.getX() * inBaysLong, inRunInXDir);
				aisle = new Aisle(this, inAisleId, inAnchorPoint, pickFaceEndPoint);
				try {
					Aisle.DAO.store(aisle);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}

				Point bayAnchorPoint = Point.getZeroPoint();
				Point aisleBoundary = Point.getZeroPoint();

				Short curLedPosNum = 1;
				Short channelNum = 1;
				for (int bayNum = 1; bayNum <= inBaysLong; bayNum++) {
					Double anchorPosZ = 0.0;
					for (int bayHighNum = 0; bayHighNum < inBaysHigh; bayHighNum++) {
						String bayName = "B" + bayNum;
						if (inBaysHigh > 1) {
							bayName += Integer.toString(bayHighNum);
						}
						bayAnchorPoint.setAnchorPosZ(anchorPosZ);
						Bay bay = createZigZagBay(aisle,
							bayName,
							inLeftHandBay,
							curLedPosNum,
							bayAnchorPoint,
							inProtoBayPoint,
							inRunInXDir,
							ledController,
							channelNum);
						aisle.addLocation(bay);

						// Get the last LED position from the bay to setup the next one.
						curLedPosNum = (short) (bay.getLastLedNumAlongPath());

						// Create the bay's boundary vertices.
						if (inRunInXDir) {
							createOrUpdateVertices(bay, new Point(PositionTypeEnum.METERS_FROM_PARENT,
								inProtoBayPoint.getX(),
								inProtoBayPoint.getY(),
								0.0));
						} else {
							createOrUpdateVertices(bay, new Point(PositionTypeEnum.METERS_FROM_PARENT,
								inProtoBayPoint.getY(),
								inProtoBayPoint.getX(),
								0.0));
						}

						anchorPosZ += inProtoBayPoint.getZ();
					}

					prepareNextBayAnchorPoint(inProtoBayPoint, inRunInXDir, bayAnchorPoint, aisleBoundary);
				}

				// Create the aisle's boundary vertices.
				createOrUpdateVertices(aisle, aisleBoundary);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inProtoBayWidthMeters
	 * @param inBaysLong
	 * @param inRunInXDir
	 * @return
	 */
	private Point computePickFaceEndPoint(final Point inAnchorPoint, final Double inDistanceMeters, final Boolean inRunInXDir) {
		Point result;
		if (inRunInXDir) {
			result = new Point(PositionTypeEnum.METERS_FROM_PARENT, inAnchorPoint.getX() + inDistanceMeters, 0.0, 0.0);
		} else {
			result = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, inAnchorPoint.getY() + inDistanceMeters, 0.0);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inProtoBayPoint
	 * @param inRunInXDir
	 * @param inAnchorPos
	 * @param inAisleBoundary
	 */
	private void prepareNextBayAnchorPoint(final Point inProtoBayPoint,
		final Boolean inRunInXDir,
		Point inAnchorPos,
		Point inAisleBoundary) {
		if (inRunInXDir) {
			if ((inAnchorPos.getX() + inProtoBayPoint.getX()) > inAisleBoundary.getX()) {
				inAisleBoundary.setX(inAnchorPos.getX() + inProtoBayPoint.getX());
			}

			if ((inAnchorPos.getY() + inProtoBayPoint.getY()) > inAisleBoundary.getY()) {
				inAisleBoundary.setY(inAnchorPos.getY() + inProtoBayPoint.getY());
			}

			inAnchorPos.setX(inAnchorPos.getX() + inProtoBayPoint.getX());
		} else {
			if ((inAnchorPos.getX() + inProtoBayPoint.getY()) > inAisleBoundary.getX()) {
				inAisleBoundary.setX(inAnchorPos.getX() + inProtoBayPoint.getY());
			}

			if ((inAnchorPos.getY() + inProtoBayPoint.getX()) > inAisleBoundary.getY()) {
				inAisleBoundary.setY(inAnchorPos.getY() + inProtoBayPoint.getX());
			}

			inAnchorPos.setY(inAnchorPos.getY() + inProtoBayPoint.getY());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Create the zig-zag LED strip bays that we see with rolling cart bays.
	 * (E.g. the bays we see at GoodEggs.)
	 *
	 * @return
	 */
	private Bay createZigZagBay(final Aisle inParentAisle,
		final String inBayId,
		final Boolean inIsLeftHandBay,
		final Short inFirstLedNum,
		final Point inAnchorPoint,
		final Point inProtoBayPoint,
		final Boolean inRunsInXDir,
		final LedController inLedController,
		final short inLedChannelNum) {

		Point bayAnchorPoint = new Point(inAnchorPoint);
		Point bayPickFacePoint = new Point(inProtoBayPoint);

		Point pickFaceEndPoint = computePickFaceEndPoint(bayAnchorPoint, bayPickFacePoint.getX(), inRunsInXDir);
		Bay resultBay = new Bay(inParentAisle, inBayId, bayAnchorPoint, pickFaceEndPoint);

		resultBay.setFirstLedNumAlongPath(inFirstLedNum);
		resultBay.setLastLedNumAlongPath((short) (inFirstLedNum + 160));

		Double bayWidth = 0.0;
		if (inRunsInXDir) {
			bayWidth = inProtoBayPoint.getX();
		} else {
			bayWidth = inProtoBayPoint.getY();
		}

		try {
			Bay.DAO.store(resultBay);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		double tierZPos = 1.25;
		short tierStartLedNum = 1;
		short tierLedCount = 32;
		boolean leftToRight = inIsLeftHandBay;
		for (int tierNum = 5; tierNum > 0; tierNum--) {
			createTier(resultBay,
				"T" + tierNum,
				leftToRight,
				inRunsInXDir,
				bayWidth,
				inProtoBayPoint.getZ(),
				tierZPos,
				inLedController,
				inLedChannelNum,
				tierStartLedNum,
				tierLedCount);
			tierZPos -= 0.25;
			tierStartLedNum += 32;
			leftToRight = !leftToRight;
		}

		try {
			Bay.DAO.store(resultBay);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return resultBay;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inParentBay
	 * @param inTierId
	 * @param inSlotsRunRight
	 * @param inRunsInXDir
	 * @param inOffset1
	 * @param inOffset2
	 * @param inLedController
	 * @param inLedChannelNum
	 */
	private void createTier(final Bay inParentBay,
		final String inTierId,
		final Boolean inSlotRunsRight,
		final Boolean inRunsInXDir,
		final Double inBayWidth,
		final Double inBayHeight,
		final Double inTierZOffset,
		final LedController inLedController,
		final Short inLedChannelNum,
		final Short inFirstLedPosNum,
		final Short inTierLedCount) {

		Point anchorPoint = Point.getZeroPoint();
		anchorPoint.translateZ(inTierZOffset);
		Point pickFaceEndPoint = computePickFaceEndPoint(anchorPoint, inBayWidth, inRunsInXDir);
		pickFaceEndPoint.translateZ(inTierZOffset);
		Tier tier = new Tier(anchorPoint, pickFaceEndPoint);

		tier.setDomainId(inTierId);
		tier.setLedController(inLedController);
		tier.setLedChannel(inLedChannelNum);
		if (inSlotRunsRight) {
			tier.setFirstLedNumAlongPath((short) (inParentBay.getFirstLedNumAlongPath() + inFirstLedPosNum - 1));
			tier.setLastLedNumAlongPath((short) (inParentBay.getFirstLedNumAlongPath() + inFirstLedPosNum + inTierLedCount - 1));
		} else {
			tier.setFirstLedNumAlongPath((short) (inParentBay.getFirstLedNumAlongPath() + inFirstLedPosNum + inTierLedCount - 1));
			tier.setLastLedNumAlongPath((short) (inParentBay.getFirstLedNumAlongPath() + inFirstLedPosNum - 1));
		}
		tier.setParent(inParentBay);
		inParentBay.addLocation(tier);
		try {
			Tier.DAO.store(tier);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		// Add slots to this tier.
		if (inSlotRunsRight) {
			createSlot(tier, "S1", inRunsInXDir, 0.0, inLedController, inLedChannelNum, (short) 1, (short) 4);
			createSlot(tier, "S2", inRunsInXDir, 0.25, inLedController, inLedChannelNum, (short) 11, (short) 8);
			createSlot(tier, "S3", inRunsInXDir, 0.5, inLedController, inLedChannelNum, (short) 15, (short) 18);
			createSlot(tier, "S4", inRunsInXDir, 0.75, inLedController, inLedChannelNum, (short) 25, (short) 22);
			createSlot(tier, "S5", inRunsInXDir, 1.0, inLedController, inLedChannelNum, (short) 29, (short) 32);
		} else {
			createSlot(tier, "S1", inRunsInXDir, 0.0, inLedController, inLedChannelNum, (short) 32, (short) 29);
			createSlot(tier, "S2", inRunsInXDir, 0.25, inLedController, inLedChannelNum, (short) 22, (short) 25);
			createSlot(tier, "S3", inRunsInXDir, 0.5, inLedController, inLedChannelNum, (short) 18, (short) 15);
			createSlot(tier, "S4", inRunsInXDir, 0.75, inLedController, inLedChannelNum, (short) 8, (short) 11);
			createSlot(tier, "S5", inRunsInXDir, 1.0, inLedController, inLedChannelNum, (short) 4, (short) 1);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inParentTier
	 * @param inSlotId
	 * @param inOffset1
	 * @param inOffset2
	 * @param inLedController
	 * @param inChannelNum
	 * @param inFirstLedNum
	 * @param inLastLedNum
	 */
	private void createSlot(final Tier inParentTier,
		final String inSlotId,
		final Boolean inRunsInXDir,
		final Double inOffset,
		final LedController inLedController,
		final Short inChannelNum,
		final Short inFirstLedPosNum,
		final Short inLastLedPosNum) {

		Point anchorPoint = Point.getZeroPoint();
		if (inRunsInXDir) {
			anchorPoint.translateX(inOffset);
		} else {
			anchorPoint.translateY(inOffset);
		}

		Point pickFaceEndPoint = computePickFaceEndPoint(anchorPoint, 0.25, inRunsInXDir);

		Slot slot = new Slot(anchorPoint, pickFaceEndPoint);

		slot.setDomainId(inSlotId);
		slot.setLedController(inLedController);
		slot.setLedChannel(inChannelNum);
		if (inParentTier.getFirstLedNumAlongPath() < inParentTier.getLastLedNumAlongPath()) {
			slot.setFirstLedNumAlongPath((short) (inParentTier.getFirstLedNumAlongPath() + inFirstLedPosNum - 1));
			slot.setLastLedNumAlongPath((short) (inParentTier.getFirstLedNumAlongPath() + inLastLedPosNum - 1));
		} else {
			slot.setFirstLedNumAlongPath((short) (inParentTier.getLastLedNumAlongPath() + inFirstLedPosNum - 1));
			slot.setLastLedNumAlongPath((short) (inParentTier.getLastLedNumAlongPath() + inLastLedPosNum - 1));
		}
		slot.setParent(inParentTier);

		inParentTier.addLocation(slot);
		try {
			Slot.DAO.store(slot);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	public final Path createPath(String inDomainId) {
		Path path = Path.create(this, inDomainId);
		this.addPath(path); // missing before. Cause of bug?
		getDao().store(this);
		path.createDefaultWorkArea(); //TODO an odd way to construct, but it is a way to make sure the Path is persisted before the work area
		return path;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a path
	 *
	 */
	@Transactional
	public final void createPath(String inDomainId, PathSegment[] inPathSegments) {
		Path path = createPath(inDomainId);
		for (PathSegment pathSegment : inPathSegments) {
			pathSegment.setParent(path);
			PathSegment.DAO.store(pathSegment);
			path.addPathSegment(pathSegment);
		}

		// Recompute the distances of the structures?
		// This does no good as the path segments are not associated to aisles yet.
		//recomputeLocationPathDistances(path);

	}

	// --------------------------------------------------------------------------
	/**
	 * A sample routine to show the distance of locations along a path.
	 */
	public final void recomputeLocationPathDistances(Path inPath) {
		// This used to do all paths. Now as advertised only the passed in path
		
		// Paul: uncomment this block, then run AisleTest.java
		// Just some debug help. Crash here sometimes as consequence of path = segment.getParent(), then pass the apparently good path to facility.recomputeLocationPathDistances(path)
		/*
		SortedSet<PathSegment> theSegments = inPath.getSegments(); // throws within getSegments if inPath reference is not fully hydrated.
		int howMany = theSegments.size();
		for (PathSegment segment : theSegments) {
			segment.computePathDistance();
			for (ILocation<?> location : segment.getLocations()) {
				location.computePosAlongPath(segment);
			}
		}
		*/
		
		// Paul: comment this block when you uncomment the block above
		// getting from paths.values() clearly does not work reliable after just making new path
		// Original code here
		// /*
		for (Path path : paths.values()) {
			for (PathSegment segment : path.getSegments()) {
				segment.computePathDistance();
				for (ILocation<?> location : segment.getLocations()) {
					location.computePosAlongPath(segment);
				}
			}
		}
		// */
		

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainId
	 * @param inPosTypeByStr
	 * @param inPosX
	 * @param inPosY
	 * @param inDrawOrder
	 */
	public final void createVertex(final String inDomainId,
		final String inPosTypeByStr,
		final Double inPosX,
		final Double inPosY,
		final Integer inDrawOrder) {

		Vertex vertex = new Vertex();
		vertex.setParent(this);
		vertex.setDomainId(inDomainId);
		vertex.setPoint(new Point(PositionTypeEnum.valueOf(inPosTypeByStr), inPosX, inPosY, null));
		vertex.setDrawOrder(inDrawOrder);
		this.addVertex(vertex);

		Vertex.DAO.store(vertex);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLocation
	 * @param inDimMeters
	 */
	public final void createOrUpdateVertices(LocationABC inLocation, Point inDimMeters) {
		// Change to public as this is called from aisle file reader, and later from editor
		// change from create to createOrUpdate
		// Maybe this should not be a facility method.

		List<Vertex> vList = inLocation.getVerticesInOrder();

		// could refactor  more into arrays and for loops. Would need to manufacture the "V01", etc.
		Point[] points = new Point[4];
		points[0] = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, null);
		points[1] = new Point(PositionTypeEnum.METERS_FROM_PARENT, inDimMeters.getX(), 0.0, null);
		points[2] = new Point(PositionTypeEnum.METERS_FROM_PARENT, inDimMeters.getX(), inDimMeters.getY(), null);
		points[3] = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, inDimMeters.getY(), null);
		String[] vertexNames = new String[4];
		vertexNames[0] = "V01";
		vertexNames[1] = "V02";
		vertexNames[2] = "V03";
		vertexNames[3] = "V04";

		int vertexListSize = vList.size();

		if (vertexListSize == 0) {
			try {
				// Create four simple vertices around the aisle.
				for (int n = 0; n < 4; n++) {
					Vertex vertexN = new Vertex(inLocation, vertexNames[n], n, points[n]);
					Vertex.DAO.store(vertexN);
					// should not be necessary. ebeans trouble?
					// Interesting bug. Drop aisle works. Redrop while still running lead to error if addVertex not there. Subsequent redrops after application start ok.
					inLocation.addVertex(vertexN);
				}
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		} else {
			try {
				// second try. Not handling < 4 yet
				for (int n = 0; n < vertexListSize; n++) {
					Vertex theVertex = vList.get(n);
					if (n >= 4) {
						// Vertex.DAO.delete(theVertex);
						LOGGER.error("extra vertex?. Why is it here?");
					} else {
						// just update the points
						// and verify the name
						String vertexName = theVertex.getDomainId();
						if (!vertexName.equals(vertexNames[n])) {
							LOGGER.error("Wrong vertex name. How?");
						}
						theVertex.setPoint(points[n]);
						Vertex.DAO.store(theVertex);
					}
				}

			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	@Transactional
	public final void createDefaultContainerKind() {
		ContainerKind containerKind = createContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND, 0.0, 0.0, 0.0);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	@Transactional
	public final ContainerKind createContainerKind(String inDomainId,
		Double inLengthMeters,
		Double inWidthMeters,
		Double inHeightMeters) {

		ContainerKind result = null;

		result = new ContainerKind();
		result.setParent(this);
		result.setDomainId(inDomainId);
		result.setLengthMeters(inLengthMeters);
		result.setWidthMeters(inWidthMeters);
		result.setHeightMeters(inHeightMeters);

		this.addContainerKind(result);
		try {
			ContainerKind.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final IronMqService getIronMqService() {
		IronMqService result = null;

		for (IEdiService ediService : getEdiServices()) {
			if (ediService instanceof IronMqService) {
				result = (IronMqService) ediService;
			}
			break;
		}

		if (result == null) {
			return createIronMqService();
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final IronMqService createIronMqService() {
		IronMqService result = null;

		result = new IronMqService();
		result.setParent(this);
		result.setDomainId("IRONMQ");
		result.setProviderEnum(EdiProviderEnum.IRONMQ);
		result.setServiceStateEnum(EdiServiceStateEnum.LINKED);

		IronMqService.Credentials credentials = result.new Credentials(IronMqService.PROJECT_ID, IronMqService.TOKEN);
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String json = gson.toJson(credentials);
		result.setProviderCredentials(json);

		this.addEdiService(result);
		try {
			IronMqService.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final DropboxService getDropboxService() {
		DropboxService result = null;

		for (IEdiService ediService : getEdiServices()) {
			if (ediService instanceof DropboxService) {
				result = (DropboxService) ediService;
			}
			break;
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	@Transactional
	public final DropboxService createDropboxService() {

		DropboxService result = null;

		result = new DropboxService();
		result.setParent(this);
		result.setDomainId("DROPBOX");
		result.setProviderEnum(EdiProviderEnum.DROPBOX);
		result.setServiceStateEnum(EdiServiceStateEnum.UNLINKED);

		this.addEdiService(result);
		try {
			DropboxService.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	@Transactional
	public final CodeshelfNetwork createNetwork(final String inNetworkName) {

		CodeshelfNetwork result = null;

		result = new CodeshelfNetwork();
		result.setParent(this);
		result.setDomainId(inNetworkName);
		result.setActive(true);
		//result.setCredential(Double.toString(Math.random()));
		result.setCredential("0.6910096026612129");
		this.addNetwork(result);

		try {
			CodeshelfNetwork.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
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
	@Transactional
	public final Integer computeWorkInstructions(final Che inChe, final List<String> inContainerIdList) {
		// TODO: Get one timestamp here

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		// Delete any planned WIs for this CHE.
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("chePersistentId", inChe.getPersistentId().toString());
		filterParams.put("type", WorkInstructionTypeEnum.PLAN.toString());
		for (WorkInstruction wi : WorkInstruction.DAO.findByFilter("assignedChe.persistentId = :chePersistentId and typeEnum = :type",
			filterParams)) {
			WorkInstruction.DAO.delete(wi);
		}

		List<Container> containerList = new ArrayList<Container>();
		for (String containerId : inContainerIdList) {
			Container container = getContainer(containerId);
			if (container != null) {
				// add to the list that will generate work instructions
				containerList.add(container);
				// Set the CHE on the containerUse
				ContainerUse thisUse = container.getCurrentContainerUse();
				if (thisUse != null) {
					thisUse.setCurrentChe(inChe);
					try {
						ContainerUse.DAO.store(thisUse);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}

			}
		}

		// TODO: pass the timestamp into generateOutboundInstructions and generateCrossWallInstructions to use as create or assign time
		Timestamp theTime = new Timestamp(System.currentTimeMillis());

		// Get all of the OUTBOUND work instructions.
		wiResultList.addAll(generateOutboundInstructions(inChe, containerList, theTime));

		// Get all of the CROSS work instructions.
		wiResultList.addAll(generateCrossWallInstructions(inChe, containerList, theTime));

		WorkInstructionSequencer sequencer = getSequencer();
		List<WorkInstruction> sortedWIResults = sequencer.sort(this, wiResultList);
		return sortedWIResults.size();
	}

	private WorkInstructionSequencer getSequencer() {
		//TODO implement short term logic for selecting strategy based on facility
		return new BayDistanceWorkInstructionSequencer();
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
	@Transactional
	public final List<WorkInstruction> getWorkInstructions(final Che inChe, final String inScannedLocationId) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		ISubLocation<?> cheLocation = null;
		if (!inScannedLocationId.isEmpty()) {
			cheLocation = findSubLocationById(inScannedLocationId);
			if (cheLocation == null) {
				LOGGER.warn("unknown CHE scan location" + inScannedLocationId);
			}
		}

		Double startingPathPos = 0.0;
		if (cheLocation != null) {
			Path path = cheLocation.getAssociatedPathSegment().getParent();
			Bay cheBay = cheLocation.getParentAtLevel(Bay.class);
			Bay selectedBay = cheBay;
			if (cheBay == null) {
				LOGGER.error("Che does not have a bay parent location in getWorkInstructions #1");
				return wiResultList;
			} else if (cheBay.getPosAlongPath() == null) {
				LOGGER.error("Ches bay parent location does not have posAlongPath in getWorkInstructions #2");
				return wiResultList;
			}

			for (Bay bay : path.<Bay> getLocationsByClass(Bay.class)) {
				// Find any bay sooner on the work path that's within 2% of this bay.
				if (bay.getPosAlongPath() == null) {
					LOGGER.error("bay location does not have posAlongPath in getWorkInstructions #3");
				} else if ((bay.getPosAlongPath() < cheBay.getPosAlongPath())
						&& (bay.getPosAlongPath() + ISubLocation.BAY_ALIGNMENT_FUDGE > cheBay.getPosAlongPath())) {
					selectedBay = bay;
				}
			}

			// Figure out the starting path position.
			startingPathPos = selectedBay.getPosAlongPath();
		}

		// Get all of the PLAN WIs assigned to this CHE beyond the specified position.
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("chePersistentId", inChe.getPersistentId().toString());
		filterParams.put("type", WorkInstructionTypeEnum.PLAN.toString());
		filterParams.put("pos", startingPathPos);
		String filter = "(assignedChe.persistentId = :chePersistentId) and (typeEnum = :type) and (posAlongPath >= :pos)";
		for (WorkInstruction wi : WorkInstruction.DAO.findByFilter(filter, filterParams)) {
			wiResultList.add(wi);
		}

		return wiResultList;
	}

	// --------------------------------------------------------------------------
	/**
	 *Utility function for outbound order WI generation
	 * @param inChe
	 * @param inContainer
	 * @param inTime
	 * @return
	 */
	private WorkInstruction makeWIForOutbound(final OrderDetail inOrderDetail,
		final Che inChe,
		final Container inContainer,
		final Timestamp inTime) {

		WorkInstruction resultWi = null;
		ItemMaster itemMaster = inOrderDetail.getItemMaster();

		if (itemMaster.getItems().size() == 0) {
			// If there is no item in inventory (AT ALL) then create a PLANNED, SHORT WI for this order detail.

			// Need to improve? Do we already have a short WI for this order detail? If so, do we really want to make another?
			// This should be moderately rare, although it happens in our test case over and over. User has to scan order/container to cart when the item master has
			// absolutely no items. Not just none in inventory, or items have zero quantity.
			resultWi = createWorkInstruction(WorkInstructionStatusEnum.SHORT,
				WorkInstructionTypeEnum.ACTUAL,
				inOrderDetail,
				inContainer,
				inChe,
				this,
				inTime);
			// above, passed this (facility) as the location of the short WI..

			if (resultWi != null) {
				resultWi.setPlanQuantity(0);
				resultWi.setPlanMinQuantity(0);
				resultWi.setPlanMaxQuantity(0);
				try {
					WorkInstruction.DAO.store(resultWi);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		} else {
			for (Path path : getPaths()) {
				// cross batch order headers have location that we are going to
				// OrderLocation firstOutOrderLoc = orderHeader.getFirstOrderLocationOnPath(path);
				// outbound order item masters have a location to pick from
				//OrderLocation firstOutOrderLoc = itemMaster.getFirstItemLocationOnPath(path);
				Item item = itemMaster.getFirstItemOnPath(path);

				if (item != null) {
					resultWi = createWorkInstruction(WorkInstructionStatusEnum.NEW,
						WorkInstructionTypeEnum.PLAN,
						inOrderDetail,
						inContainer,
						inChe,
						(ISubLocation<?>) item.getStoredLocation(),
						inTime);
				}
				// Bug here, especially if we don't worry about matching uom. Might find the same items on two paths: one case and one each.
			}
		}
		return resultWi;
	}

	// --------------------------------------------------------------------------
	/**
	 * Generate pick work instructions for a container at a specific location on a path.
	 * @param inChe
	 * @param inContainerList
	 * @param inTime
	 * @return
	 */
	private List<WorkInstruction> generateOutboundInstructions(final Che inChe,
		final List<Container> inContainerList,
		final Timestamp inTime) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		// To proceed, there should container use linked to outbound order
		// We want to add all orders represented in the container list because these containers (or for Accu, fake containers representing the order) were scanned for this CHE to do.
		for (Container container : inContainerList) {
			OrderHeader order = container.getCurrentOrderHeader();
			if (order != null && order.getOrderTypeEnum().equals(OrderTypeEnum.OUTBOUND)) {
				boolean somethingDone = false;
				for (OrderDetail orderDetail : order.getOrderDetails()) {
					// An order detail might be set to zero quantity by customer, essentially canceling that item. Don't make a WI if canceled.
					if (orderDetail.getQuantity() > 0) {
						WorkInstruction aWi = makeWIForOutbound(orderDetail, inChe, container, inTime); // Could be normal WI, or a short WI
						if (aWi != null) {
							wiResultList.add(aWi);
							somethingDone = true;

							// still do this for a short WI?
							orderDetail.setStatusEnum(OrderStatusEnum.INPROGRESS);
							try {
								OrderDetail.DAO.store(orderDetail);
							} catch (DaoException e) {
								LOGGER.error("", e);
							}

						}
					}
				}
				if (somethingDone) {
					order.setStatusEnum(OrderStatusEnum.INPROGRESS);
					try {
						OrderHeader.DAO.store(order);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}
			}
		}
		return wiResultList;
		// Need an accu-sort instead of this.

		/*  old signature was this followed by old code. Above is vastly different
		private List<WorkInstruction> generateOutboundInstructions(final Che inChe,
			final List<Container> inContainerList,
			final Path inPath,
			final String inScannedLocationId,
			final ISubLocation<?> inCheLocation,
			final Timestamp inTime) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		for (Container container : inContainerList) {
			OrderHeader order = container.getCurrentOrderHeader();
			if (order != null) {
				for (OrderDetail orderDetail : order.getOrderDetails()) {
					ItemMaster itemMaster = orderDetail.getItemMaster();

					// Figure out if there are any items are on the current path.
					// (We just take the first one we find, because items slotted on the same path should be close together.)
					Item selectedItem = null;

					if (itemMaster.getItems().size() == 0) {
						// If there is no item in inventory (AT ALL) then create a PLANEED, SHORT WI for this order detail.
						WorkInstruction plannedWi = createWorkInstruction(WorkInstructionStatusEnum.SHORT,
							WorkInstructionTypeEnum.ACTUAL,
							orderDetail,
							container,
							inChe,
							inCheLocation,
							inTime);
						if (plannedWi != null) {
							plannedWi.setPlanQuantity(0);
							plannedWi.setPlanMinQuantity(0);
							plannedWi.setPlanMaxQuantity(0);
							try {
								WorkInstruction.DAO.store(plannedWi);
							} catch (DaoException e) {
								LOGGER.error("", e);
							}
							wiResultList.add(plannedWi);
						}
					} else {
						// Search through the items to see if any are on the CHE's pick path.
						ISubLocation<IDomainObject> foundLocation = null;
						for (Item item : itemMaster.getItems()) {
							if (item.getStoredLocation() instanceof ISubLocation) {
								ISubLocation location = (ISubLocation) item.getStoredLocation();
								if (inPath.isLocationOnPath(location)) {
									foundLocation = location;
									selectedItem = item;
									break;
								}
							}
						}

						// The item is on the CHE's path, so add it.
						if (foundLocation != null) {
							// If there is anything to pick on this item then create a WI for it.
							if (orderDetail.getQuantity() > 0) {
								WorkInstruction plannedWi = createWorkInstruction(WorkInstructionStatusEnum.NEW,
									WorkInstructionTypeEnum.PLAN,
									orderDetail,
									container,
									inChe,
									foundLocation,
									inTime);
								if (plannedWi != null) {
									wiResultList.add(plannedWi);
								}

								orderDetail.setStatusEnum(OrderStatusEnum.INPROGRESS);
								try {
									OrderDetail.DAO.store(orderDetail);
								} catch (DaoException e) {
									LOGGER.error("", e);
								}

								order.setStatusEnum(OrderStatusEnum.INPROGRESS);
								try {
									OrderHeader.DAO.store(order);
								} catch (DaoException e) {
									LOGGER.error("", e);
								}
							}
						}
					}
				}
			}
		}

		// If we found WIs then sort them by they distance from the named location (closest first).
		if (wiResultList.size() > 0) {
			inPath.sortWisByDistance(wiResultList);
		}

		return wiResultList;
		*/
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
	private List<WorkInstruction> generateCrossWallInstructions(final Che inChe,
		final List<Container> inContainerList,
		final Timestamp inTime) {

		List<WorkInstruction> wiList = new ArrayList<WorkInstruction>();

		for (Container container : inContainerList) {
			// Iterate over all active CROSS orders on the path.
			OrderHeader crossOrder = container.getCurrentOrderHeader();
			if ((crossOrder != null) && (crossOrder.getActive()) && (crossOrder.getOrderTypeEnum().equals(OrderTypeEnum.CROSS))) {
				// Iterate over all active OUTBOUND on the path.
				for (OrderHeader outOrder : getOrderHeaders()) {
					if ((outOrder.getOrderTypeEnum().equals(OrderTypeEnum.OUTBOUND)) && (outOrder.getActive())) {
						// Only use orders without an order group, or orders in the same order group as the cross order.
						if (((outOrder.getOrderGroup() == null) && (crossOrder.getOrderGroup() == null))
								|| (outOrder.getOrderGroup() != null) && (outOrder.getOrderGroup().equals(crossOrder.getOrderGroup()))) {
							// OK, we have an OUTBOUND order on the same path as the CROSS order.
							// Check to see if any of the active CROSS order detail items match OUTBOUND order details.
							for (OrderDetail crossOrderDetail : crossOrder.getOrderDetails()) {
								if (crossOrderDetail.getActive()) {
									for (OrderDetail outOrderDetail : outOrder.getOrderDetails()) {
										if ((outOrderDetail.getItemMaster().equals(crossOrderDetail.getItemMaster()))
												&& (outOrderDetail.getActive())) {
											// Now make sure the UOM matches.
											if (outOrderDetail.getUomMasterId().equals(crossOrderDetail.getUomMasterId())) {
												for (Path path : getPaths()) {
													OrderLocation firstOutOrderLoc = outOrder.getFirstOrderLocationOnPath(path);

													if (firstOutOrderLoc != null) {
														WorkInstruction wi = createWorkInstruction(WorkInstructionStatusEnum.NEW,
															WorkInstructionTypeEnum.PLAN,
															outOrderDetail,
															container,
															inChe,
															firstOutOrderLoc.getLocation(),
															inTime);

														// If we created a WI then add it to the list.
														if (wi != null) {
															setWiPickInstruction(wi, outOrder);
															wiList.add(wi);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return wiList;
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

		// old way. Not sorted. Just took the locations on the order in whatever order they were.
		/*
		for (OrderLocation orderLocation : inOrder.getOrderLocations()) {
			LocationAlias locAlias = orderLocation.getLocation().getPrimaryAlias();
			if (locAlias != null) {
				locationString += locAlias.getAlias() + " ";
			} else {
				locationString += orderLocation.getLocation().getLocationId();
			}
		}
		*/
		for (OrderLocation orderLocation : inOrder.getOrderLocations()) {
			LocationAlias locAlias = orderLocation.getLocation().getPrimaryAlias();
			if (locAlias != null) {
				locIdList.add(locAlias.getAlias());
			} else {
				locIdList.add(orderLocation.getLocation().getLocationId());
			}
		}
		// new way. Not sorted. Simple alpha sort. Will fail on D-10 D-11 D-9
		Collections.sort(locIdList);
		for (String aString : locIdList) {
			locationString += aString + " ";
		}
		// end DEV-315 modification

		inWi.setPickInstruction(locationString);

		try {
			WorkInstruction.DAO.store(inWi);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * Compare Work Instructions by their ItemIds.
	 *
	 */
	private class WiItemIdComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			return inWi1.getItemMaster().getItemId().compareTo(inWi2.getItemMaster().getItemId());
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * Sort a list of work instructions on a path through a CrossWall
	 * @param inCrosswallWiList
	 * @param inBays
	 * @return
	 */
	private List<WorkInstruction> sortCrosswallInstructionsInLocationOrder(final List<WorkInstruction> inCrosswallWiList,
		final List<ISubLocation<?>> inSubLocations) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		// Cycle over all bays on the path.
		for (ISubLocation<?> subLocation : inSubLocations) {
			for (ILocation<?> workLocation : subLocation.getSubLocationsInWorkingOrder()) {
				Iterator<WorkInstruction> wiIterator = inCrosswallWiList.iterator();
				while (wiIterator.hasNext()) {
					WorkInstruction wi = wiIterator.next();
					if (wi.getLocation().equals(workLocation)) {
						String wantedItemId = wi.getItemMaster().getItemId();
						wiResultList.add(wi);
						wi.setGroupAndSortCode(String.format("%04d", wiResultList.size()));
						WorkInstruction.DAO.store(wi);
						wiIterator.remove();
					}
				}
			}
		}
		return wiResultList;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a work instruction for and order item quantity picked into a container at a location.
	 * @param inStatus
	 * @param inOrderDetail
	 * @param inQuantityToPick
	 * @param inContainer
	 * @param inLocation
	 * @param inPosALongPath
	 * @return
	 */
	private WorkInstruction createWorkInstruction(WorkInstructionStatusEnum inStatus,
		WorkInstructionTypeEnum inType,
		OrderDetail inOrderDetail,
		Container inContainer,
		Che inChe,
		ISubLocation<?> inLocation,
		final Timestamp inTime) {
		WorkInstruction resultWi = null;

		Integer qtyToPick = inOrderDetail.getQuantity();
		Integer minQtyToPick = inOrderDetail.getMinQuantity();
		Integer maxQtyToPick = inOrderDetail.getMaxQuantity();

		for (WorkInstruction wi : inOrderDetail.getWorkInstructions()) {
			if (wi.getTypeEnum().equals(WorkInstructionTypeEnum.PLAN)) {
				resultWi = wi;
				break;
			} else if (wi.getTypeEnum().equals(WorkInstructionTypeEnum.ACTUAL)) {
				// Deduct any WIs already completed for this line item.
				qtyToPick -= wi.getActualQuantity();
				minQtyToPick = Math.max(0, minQtyToPick - wi.getActualQuantity());
				maxQtyToPick = Math.max(0, maxQtyToPick - wi.getActualQuantity());
			}
		}

		// Check if there is any left to pick.
		if (qtyToPick > 0) {

			// If there is no planned WI then create one.
			if (resultWi == null) {
				resultWi = new WorkInstruction();
				resultWi.setParent(inOrderDetail);
				resultWi.setCreated(new Timestamp(System.currentTimeMillis()));
			}

			// Set the LED lighting pattern for this WI.
			if (inOrderDetail.getParent().getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) {
				setCrossWorkInstructionLedPattern(resultWi, inOrderDetail.getItemMasterId(), inLocation);
			} else {
				setOutboundWorkInstructionLedPattern(resultWi, inOrderDetail.getParent());
			}

			// Update the WI
			resultWi.setDomainId(Long.toString(System.currentTimeMillis()));
			resultWi.setTypeEnum(inType);
			resultWi.setStatusEnum(inStatus);

			resultWi.setLocation(inLocation);
			resultWi.setLocationId(inLocation.getFullDomainId());
			resultWi.setItemMaster(inOrderDetail.getItemMaster());
			String cookedDesc = inOrderDetail.getItemMaster().getDescription();
			cookedDesc = cookedDesc.substring(0, Math.min(MAX_WI_DESC_BYTES, cookedDesc.length()));
			cookedDesc = cookedDesc.replaceAll("[^\\p{ASCII}]", "");
			resultWi.setDescription(cookedDesc);
			if (inOrderDetail.getItemMaster().getDdcId() != null) {
				resultWi.setPickInstruction(inOrderDetail.getItemMaster().getDdcId());
			} else {
				LocationAlias locAlias = resultWi.getLocation().getPrimaryAlias();
				if (locAlias != null) {
					resultWi.setPickInstruction(locAlias.getAlias());
				} else {
					resultWi.setPickInstruction(resultWi.getLocationId());
				}
			}
			resultWi.setPosAlongPath(inLocation.getPosAlongPath());
			resultWi.setContainer(inContainer);
			resultWi.setAssignedChe(inChe);
			resultWi.setPlanQuantity(qtyToPick);
			resultWi.setPlanMinQuantity(minQtyToPick);
			resultWi.setPlanMaxQuantity(maxQtyToPick);
			resultWi.setActualQuantity(0);
			resultWi.setAssigned(inTime);
			try {
				WorkInstruction.DAO.store(resultWi);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
		return resultWi;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inOrder
	 */
	private void setOutboundWorkInstructionLedPattern(final WorkInstruction inWi, final OrderHeader inOrder) {

		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();
		for (OrderLocation orderLocation : inOrder.getOrderLocations()) {
			if (orderLocation.getActive()) {
				short firstLedPosNum = orderLocation.getLocation().getFirstLedNumAlongPath();
				short lastLedPosNum = orderLocation.getLocation().getLastLedNumAlongPath();

				// Put the positions into increasing order.
				if (firstLedPosNum > lastLedPosNum) {
					Short temp = firstLedPosNum;
					firstLedPosNum = lastLedPosNum;
					lastLedPosNum = temp;
				}

				// The new way of sending LED data to the remote controller. Note getEffectiveXXX instead of getLedController
				List<LedSample> ledSamples = new ArrayList<LedSample>();
				LedCmdGroup ledCmdGroup = new LedCmdGroup(orderLocation.getLocation()
					.getEffectiveLedController()
					.getDeviceGuidStr(), orderLocation.getLocation().getEffectiveLedChannel(), firstLedPosNum, ledSamples);

				for (short ledPos = firstLedPosNum; ledPos < lastLedPosNum; ledPos++) {
					LedSample ledSample = new LedSample(ledPos, ColorEnum.BLUE);
					ledSamples.add(ledSample);
				}
				ledCmdGroup.setLedSampleList(ledSamples);
				ledCmdGroupList.add(ledCmdGroup);
			}
		}
		inWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
	}

	// --------------------------------------------------------------------------
	/**
	 * Create the LED lighting pattern for the WI.
	 * @param inWi
	 * @param inOrderType
	 * @param inItemId
	 * @param inLocation
	 */
	private void setCrossWorkInstructionLedPattern(final WorkInstruction inWi,
		final String inItemId,
		final ISubLocation<?> inLocation) {

		short firstLedPosNum = inLocation.getFirstLedPosForItemId(inItemId);
		short lastLedPosNum = inLocation.getLastLedPosForItemId(inItemId);

		// Put the positions into increasing order.
		if (firstLedPosNum > lastLedPosNum) {
			Short temp = firstLedPosNum;
			firstLedPosNum = lastLedPosNum;
			lastLedPosNum = temp;
		}

		// The new way of sending LED data to the remote controller.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();
		LedCmdGroup ledCmdGroup = new LedCmdGroup(inLocation.getEffectiveLedController().getDeviceGuidStr(),
			inLocation.getEffectiveLedChannel(),
			firstLedPosNum,
			ledSamples);

		for (short ledPos = firstLedPosNum; ledPos < lastLedPosNum; ledPos++) {
			LedSample ledSample = new LedSample(ledPos, ColorEnum.BLUE);
			ledSamples.add(ledSample);
		}
		ledCmdGroup.setLedSampleList(ledSamples);

		ledCmdGroupList.add(ledCmdGroup);
		inWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
	}

	/**
	 * Compare Items by their ItemMasterDdc.
	 *
	 */
	private class DdcItemComparator implements Comparator<Item> {

		public int compare(Item inItem1, Item inItem2) {
			return inItem1.getParent().getDdcId().compareTo(inItem2.getParent().getDdcId());
		}
	};

	/**
	 * Compare ItemMasters by their DDC.
	 */
	private class DdcItemMasterComparator implements Comparator<ItemMaster> {

		public int compare(ItemMaster inItemMaster1, ItemMaster inItemMaster2) {
			return inItemMaster1.getDdcId().compareTo(inItemMaster2.getDdcId());
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * After a change in DDC items we call this routine to recompute the path-relative positions of the items.
	 *
	 */
	public final void recomputeDdcPositions() {

		LOGGER.debug("Begin DDC position recompute");

		List<ILocation<?>> ddcLocations = getDdcLocations();
		List<ItemMaster> ddcItemMasters = getDccItemMasters();

		// Sort the DDC items in lex/DDC order.
		LOGGER.debug("DDC sort items");
		Collections.sort(ddcItemMasters, new DdcItemMasterComparator());

		// Get the items that belong to each DDC location.
		LOGGER.debug("DDC list items");
		List<Item> locationItems = new ArrayList<Item>();
		Double locationItemsQuantity;
		for (ILocation<?> location : ddcLocations) {
			// Delete all of the old DDC groups from this location.
			for (ItemDdcGroup ddcGroup : location.getDdcGroups()) {
				ItemDdcGroup.DAO.delete(ddcGroup);
			}

			locationItemsQuantity = getLocationDdcItemsAndTotalQuantity(ddcItemMasters, locationItems, location);

			Double locationLen = computeLengthOfLocationFace(location);

			putDdcItemsInPositionOrder(locationItems, locationItemsQuantity, location, locationLen);
		}

		LOGGER.debug("End DDC position recompute");

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLocationItems
	 * @param inLocationItemsQuantity
	 * @param inLocation
	 * @param inLocationLen
	 */
	private void putDdcItemsInPositionOrder(List<Item> inLocationItems,
		Double inLocationItemsQuantity,
		ILocation<?> inLocation,
		Double inLocationLen) {

		Double ddcPos = inLocation.getPosAlongPath();
		Double distPerItem = inLocationLen / inLocationItemsQuantity;
		ItemDdcGroup lastDdcGroup = null;
		Collections.sort(inLocationItems, new DdcItemComparator());
		for (Item item : inLocationItems) {
			ddcPos += distPerItem * item.getQuantity();
			item.setPosAlongPath(ddcPos);
			try {
				item.DAO.store(item);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

			// Figure out if we've changed DDC group codes and start a new group.
			if ((lastDdcGroup == null) || (!lastDdcGroup.getDdcGroupId().equals(item.getParent().getDdcId()))) {

				// Finish the end position of the last DDC group and store it.
				if (lastDdcGroup != null) {
					ItemDdcGroup.DAO.store(lastDdcGroup);
				}

				// Start the next DDC group.
				lastDdcGroup = new ItemDdcGroup();
				lastDdcGroup.setDdcGroupId(item.getParent().getDdcId());
				lastDdcGroup.setParent(item.getStoredLocation());
				lastDdcGroup.setStartPosAlongPath(item.getPosAlongPath());
			}
			lastDdcGroup.setEndPosAlongPath(item.getPosAlongPath());
		}
		// Store the last DDC
		if (lastDdcGroup != null) {
			ItemDdcGroup.DAO.store(lastDdcGroup);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDdcItemMasters
	 * @param inLocationItems
	 * @param inLocation
	 * @return
	 */
	private Double getLocationDdcItemsAndTotalQuantity(List<ItemMaster> inDdcItemMasters,
		List<Item> inLocationItems,
		ILocation<?> inLocation) {
		Double locationItemCount;
		LOGGER.debug("DDC location check: " + inLocation.getFullDomainId() + " " + inLocation.getPersistentId());
		inLocationItems.clear();
		locationItemCount = 0.0;
		for (ItemMaster itemMaster : inDdcItemMasters) {
			if ((itemMaster.getDdcId().compareTo(inLocation.getFirstDdcId()) >= 0)
					&& (itemMaster.getDdcId().compareTo(inLocation.getLastDdcId()) <= 0)) {
				for (Item item : itemMaster.getItems()) {
					LOGGER.debug("DDC assign item: " + "loc: " + inLocation.getFullDomainId() + " itemId: "
							+ itemMaster.getItemId() + " Ddc: " + item.getParent().getDdcId());
					item.setStoredLocation(inLocation);
					inLocation.addStoredItem(item);
					inLocationItems.add(item);
					locationItemCount += item.getQuantity();
				}
			}
		}
		return locationItemCount;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLocation
	 * @return
	 */
	private Double computeLengthOfLocationFace(ILocation<?> inLocation) {
		Double locationLen = 0.0;
		Vertex lastVertex = null;
		List<Vertex> list = inLocation.getVertices();
		for (Vertex vertex : list) {
			if (lastVertex != null) {
				if (Math.abs(vertex.getPosX() - lastVertex.getPosX()) > locationLen) {
					locationLen = Math.abs(vertex.getPosX() - lastVertex.getPosX());
				}
				if (Math.abs(vertex.getPosY() - lastVertex.getPosY()) > locationLen) {
					locationLen = Math.abs(vertex.getPosY() - lastVertex.getPosY());
				}
			}
			lastVertex = vertex;
		}
		return locationLen;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private List<ItemMaster> getDccItemMasters() {
		LOGGER.debug("DDC get items");
		List<ItemMaster> ddcItemMasters = new ArrayList<ItemMaster>();
		for (ItemMaster itemMaster : getItemMasters()) {
			if ((itemMaster.getDdcId() != null) && (itemMaster.getActive())) {
				ddcItemMasters.add(itemMaster);
			}
		}
		return ddcItemMasters;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private List<ILocation<?>> getDdcLocations() {
		LOGGER.debug("DDC get locations");
		List<ILocation<?>> ddcLocations = new ArrayList<ILocation<?>>();
		for (Aisle aisle : getAisles()) {
			for (ILocation<?> location : aisle.getChildren()) {
				if (location.getFirstDdcId() != null) {
					ddcLocations.add(location);
				}
			}
		}
		return ddcLocations;
	}

	// --------------------------------------------------------------------------
	/**
	 * Used in aisle file read to decide if we should automatically make more controllers
	 */
	public final int countLedControllers() {
		int result = 0;

		CodeshelfNetwork network = getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_ID);
		if (network == null)
			return result;
		Map<String, LedController> controllerMap = network.getLedControllers();
		result = controllerMap.size();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * This is first done via "inferred parameter"; look at the data to determine the answer. Might change to explicit parameter later.
	 * The UI needs this answer. UI gets it at login.
	 */
	@JsonProperty("hasCrossBatchOrders")
	public final boolean hasCrossBatchOrders() {
		boolean result = false;
		for (OrderHeader theOrder : getOrderHeaders()) {
			if ((theOrder.getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) && (theOrder.getActive())) {
				result = true;
				break;
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * This is first done via "inferred parameter"; look at the data to determine the answer. Might change to explicit parameter later.
	 * The UI needs this answer. UI gets it at login.
	 * If true, the UI wants to believe that ALL crossbatch and outbound orders have an order group. The orders view will not show at all any orders without a group.
	 */
	@JsonProperty("hasMeaningfulOrderGroups")
	public final boolean hasMeaningfulOrderGroups() {

		List<OrderGroup> groupsList = this.getOrderGroups();
		boolean result = groupsList.size() > 0;
		// clearly might give the wrong value if site is initially misconfigured. Could look at the orderHeaders in more detail. Do most have groups?

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inChe
	 * @param inContainers
	 * Testing only!  passs in as 23,46,2341a23. This yields conatiner ID 23 in slot1, container Id 46 in slot 2, etc.
	 *
	 */
	public final void setUpCheContainerFromString(Che inChe, String inContainers) {
		if (inChe == null)
			return;

		// computeWorkInstructions wants a containerId list
		List<String> containersIdList = Arrays.asList(inContainers.split("\\s*,\\s*")); // this trims out white space

		if (containersIdList.size() > 0) {
			Integer wiCount = this.computeWorkInstructions(inChe, containersIdList);
			// That did the work. Big side effect.

			// Get the work instructions for this CHE at this location for the given containers. Can we pass empty string? Normally user would scan where the CHE is starting.
			List<WorkInstruction> wiList = this.getWorkInstructions(inChe, "");
			Integer wiCountGot = wiList.size();
			// getWorkInstructions() has no side effects. But the site controller request gets these.
			// As work instructions are executed, they come back with start and complete time. and PLAN/NEW changes to ACTUAL/COMPLETE or ACTUAL/SHORT
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param outHeaderCounts
	 */
	public final HeaderCounts countCrossOrders() {
		int totalCrossHeaders = 0;
		int activeHeaders = 0;
		int activeDetails = 0;
		int activeCntrUses = 0;

		for (OrderHeader crossOrder : getOrderHeaders()) {
			if (crossOrder.getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) {
				totalCrossHeaders++;
				if (crossOrder.getActive()) {
					activeHeaders++;
					ContainerUse cntrUse = crossOrder.getContainerUse();
					if (cntrUse != null && cntrUse.getActive())
						activeCntrUses++;
					for (OrderDetail crossOrderDetail : crossOrder.getOrderDetails()) {
						if (crossOrderDetail.getActive()) {
							activeDetails++;
							// if we were doing outbound orders, we might count WI here
						}
					}
				}
			}
		}
		HeaderCounts outHeaderCounts = new HeaderCounts();
		outHeaderCounts.mTotalHeaders = totalCrossHeaders;
		outHeaderCounts.mActiveHeaders = activeHeaders;
		outHeaderCounts.mActiveDetails = activeDetails;
		outHeaderCounts.mActiveCntrUses = activeCntrUses;
		return outHeaderCounts;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWorkInstruction
	 */
	public final void sendWorkInstructionsToHost(final List<WorkInstruction> inWiList) {
		IronMqService ironMqService = getIronMqService();

		if (ironMqService != null) {
			ironMqService.sendWorkInstructionsToHost(inWiList);
		}
	}
}
