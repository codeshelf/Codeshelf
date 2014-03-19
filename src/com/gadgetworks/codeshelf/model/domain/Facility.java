/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.annotation.Transactional;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;
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
public class Facility extends LocationABC<Organization> {

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

	private static final Logger				LOGGER			= LoggerFactory.getLogger(Facility.class);

	//	// The owning organization.
	//	@Column(nullable = false)
	//	@ManyToOne(optional = false)
	//	@Getter
	//	private Organization				parentOrganization;

	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility						parent;

	@OneToMany(mappedBy = "parent")
	@Getter
	private List<Aisle>						aisles			= new ArrayList<Aisle>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, Container>			containers		= new HashMap<String, Container>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
	@MapKey(name = "domainId")
	private Map<String, ContainerKind>		containerKinds	= new HashMap<String, ContainerKind>();

	@OneToMany(mappedBy = "parent", targetEntity = DropboxService.class)
	@Getter
	private List<IEdiService>				ediServices		= new ArrayList<IEdiService>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, ItemMaster>			itemMasters		= new HashMap<String, ItemMaster>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, CodeshelfNetwork>	networks		= new HashMap<String, CodeshelfNetwork>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, OrderGroup>			orderGroups		= new HashMap<String, OrderGroup>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, OrderHeader>		orderHeaders	= new HashMap<String, OrderHeader>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, Path>				paths			= new HashMap<String, Path>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, UomMaster>			uomMasters		= new HashMap<String, UomMaster>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, LocationAlias>		locationAliases	= new HashMap<String, LocationAlias>();

	public Facility() {

	}

	public Facility(final Point inAnchorPoint) {
		super(inAnchorPoint);
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

	public final Organization getParent() {
		return getParentOrganization();
	}

	public final void setParent(Organization inParentOrganization) {
		setParentOrganization(inParentOrganization);
		// There's no way in Ebean to enforce non-nullability and foreign key constraints in a tree of like-class objects.
		// So the top-level location has to be its own parent.  (Otherwise we have forego the constraints.)
		parent = this;
	}

	public final String getParentOrganizationID() {
		String result = "";
		if (getParentOrganization() != null) {
			result = getParentOrganization().getDomainId();
		}
		return result;
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
		final Double inPosXMeters,
		final Double inPosYMeters,
		final Double inProtoBayWidthMeters,
		final Double inProtoBayDepthMeters,
		final Double inProtoBayHeightMeters,
		final Integer inBaysHigh,
		final Integer inBaysLong,
		final Boolean inRunInXDir,
		final Boolean inOpensLowSide,
		final Boolean inLeftHandBay) {

		final String inLedControllerId = "0x00000002";

		// Create at least one aisle controller.
		CodeshelfNetwork network = networks.get(CodeshelfNetwork.DEFAULT_NETWORK_ID);
		if (network != null) {
			LedController ledController = network.getLedController("LED1");
			if (ledController == null) {
				ledController = network.createLedController(inLedControllerId, new NetGuid(inLedControllerId));
			}
			// Create the aisle if it doesn't already exist.
			Aisle aisle = Aisle.DAO.findByDomainId(this, inAisleId);
			if (aisle == null) {
				Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, inPosXMeters, inPosYMeters, 0.0);
				Point pickFaceEndPoint = computePickFaceEndPoint(anchorPoint, inProtoBayWidthMeters * inBaysLong, inRunInXDir);
				aisle = new Aisle(this, inAisleId, anchorPoint, pickFaceEndPoint);
				try {
					Aisle.DAO.store(aisle);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}

				Point bayAnchorPosition = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0);
				Point aisleBoundary = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0);

				Short curLedPosNum = 1;
				Short channelNum = 1;
				for (int bayNum = 1; bayNum <= inBaysLong; bayNum++) {
					Double anchorPosZ = 0.0;
					for (int bayHighNum = 0; bayHighNum < inBaysHigh; bayHighNum++) {
						String bayName = "B" + bayNum + "-" + bayHighNum;
						Bay bay = createZigZagBay(aisle,
							bayName,
							inLeftHandBay,
							curLedPosNum,
							bayAnchorPosition,
							inProtoBayWidthMeters,
							inProtoBayHeightMeters,
							inRunInXDir,
							ledController,
							channelNum);
						aisle.addLocation(bay);

						// Get the last LED position from the bay to setup the next one.
						curLedPosNum = (short) (bay.getLastLedNumAlongPath());

						// Create the bay's boundary vertices.
						if (inRunInXDir) {
							createVertices(bay, new Point(PositionTypeEnum.METERS_FROM_PARENT,
								inProtoBayWidthMeters,
								inProtoBayDepthMeters,
								0.0));
						} else {
							createVertices(bay, new Point(PositionTypeEnum.METERS_FROM_PARENT,
								inProtoBayDepthMeters,
								inProtoBayWidthMeters,
								0.0));
						}

						anchorPosZ += inProtoBayHeightMeters;
					}

					prepareNextBayAnchorPoint(inProtoBayWidthMeters,
						inProtoBayDepthMeters,
						inRunInXDir,
						bayAnchorPosition,
						aisleBoundary);
				}

				// Create the aisle's boundary vertices.
				createVertices(aisle, aisleBoundary);

				// Create the paths related to this aisle.
				aisle.createPaths(aisleBoundary.getX(), aisleBoundary.getY(), TravelDirectionEnum.FORWARD, inOpensLowSide);

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
			result = new Point(PositionTypeEnum.METERS_FROM_PARENT, inDistanceMeters, 0.0, 0.0);
		} else {
			result = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, inDistanceMeters, 0.0);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inProtoBayWidthMeters
	 * @param inProtoBayDepthMeters
	 * @param inRunInXDir
	 * @param inAnchorPos
	 * @param inAisleBoundary
	 */
	private void prepareNextBayAnchorPoint(final Double inProtoBayWidthMeters,
		final Double inProtoBayDepthMeters,
		final Boolean inRunInXDir,
		Point inAnchorPos,
		Point inAisleBoundary) {
		if (inRunInXDir) {
			if ((inAnchorPos.getX() + inProtoBayWidthMeters) > inAisleBoundary.getX()) {
				inAisleBoundary.setX(inAnchorPos.getX() + inProtoBayWidthMeters);
			}

			if ((inAnchorPos.getY() + inProtoBayDepthMeters) > inAisleBoundary.getY()) {
				inAisleBoundary.setY(inAnchorPos.getY() + inProtoBayDepthMeters);
			}

			inAnchorPos.setX(inAnchorPos.getX() + inProtoBayWidthMeters);
		} else {
			if ((inAnchorPos.getX() + inProtoBayDepthMeters) > inAisleBoundary.getX()) {
				inAisleBoundary.setX(inAnchorPos.getX() + inProtoBayDepthMeters);
			}

			if ((inAnchorPos.getY() + inProtoBayWidthMeters) > inAisleBoundary.getY()) {
				inAisleBoundary.setY(inAnchorPos.getY() + inProtoBayWidthMeters);
			}

			inAnchorPos.setY(inAnchorPos.getY() + inProtoBayDepthMeters);
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
		final Double inBayWidth,
		final Double inBayHeight,
		final Boolean inRunsInXDir,
		final LedController inLedController,
		final short inLedChannelNum) {

		Point pickFaceEndPoint = computePickFaceEndPoint(inAnchorPoint, inBayWidth, inRunsInXDir);
		Bay resultBay = new Bay(inParentAisle, inBayId, inAnchorPoint, pickFaceEndPoint);

		resultBay.setFirstLedNumAlongPath(inFirstLedNum);
		resultBay.setLastLedNumAlongPath((short) (inFirstLedNum + 160));

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
				inBayWidth,
				inBayHeight,
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

		Point anchorPoint = new Point(inParentBay.getAnchorPoint());
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
			createSlot(tier, "S1", inRunsInXDir, 0.0, inLedController, inLedChannelNum, (short) 2, (short) 9);
			createSlot(tier, "S2", inRunsInXDir, 0.25, inLedController, inLedChannelNum, (short) 11, (short) 18);
			createSlot(tier, "S3", inRunsInXDir, 0.5, inLedController, inLedChannelNum, (short) 20, (short) 26);
			createSlot(tier, "S4", inRunsInXDir, 1.0, inLedController, inLedChannelNum, (short) 28, (short) 32);
		} else {
			createSlot(tier, "S1", inRunsInXDir, 0.0, inLedController, inLedChannelNum, (short) 31, (short) 24);
			createSlot(tier, "S2", inRunsInXDir, 0.25, inLedController, inLedChannelNum, (short) 22, (short) 15);
			createSlot(tier, "S3", inRunsInXDir, 0.5, inLedController, inLedChannelNum, (short) 13, (short) 6);
			createSlot(tier, "S4", inRunsInXDir, 1.0, inLedController, inLedChannelNum, (short) 4, (short) 1);
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

		Point anchorPoint = inParentTier.getAnchorPoint();
		Point pickFaceEndPoint = computePickFaceEndPoint(anchorPoint, 0.25, inRunsInXDir);
		
		Slot slot =  new Slot(anchorPoint, pickFaceEndPoint);

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

	// --------------------------------------------------------------------------
	/**
	 * A sample routine to show the distance of locations along a path.
	 */
	public final void logLocationDistances() {
		// List out Bays by distance from initiation point.
		Path path = paths.get(Path.DEFAULT_FACILITY_PATH_ID);
		if (path != null) {
			//			int distFromInitiationPoint = 0;
			for (PathSegment segment : path.getSegments()) {
				segment.computePathDistance();
				for (ILocation location : segment.getLocations()) {
					if (location instanceof Aisle) {
						Aisle aisle = (Aisle) location;
						aisle.computePathDistance();
						for (Bay bay : aisle.<Bay> getChildrenAtLevel(Bay.class)) {
							//							// Figure out the distance of this bay from the path.
							//							Point bayPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, aisle.getPosX() + bay.getPosX(), aisle.getPosY() + bay.getPosY(), null);
							//							Double distance = distFromInitiationPoint + segment.computeDistanceOfPointFromLine(segment.getEndPoint(), segment.getStartPoint(), bayPoint);
							//
							//							String distanceStr = String.format("%4.4f", distance);
							//							LOGGER.info("Location: " + bay.getFullDomainId() + " is " + distanceStr + " meters from the initiation point.");

							bay.computePathDistance();
							LOGGER.info("Location: " + bay.getFullDomainId() + " is " + bay.getPosAlongPath()
									+ " meters from the initiation point.");
						}
					}
				}
				//				distFromInitiationPoint += segment.getLength();
			}
		}
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
	 * @param inXDimMeters
	 * @param inYDimMeters
	 */
	private void createVertices(ILocation inLocation, Point inDimMeters) {
		try {
			// Create four simple vertices around the aisle.
			Vertex vertex1 = new Vertex(inLocation, "V01", 0, new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, null));
			Vertex.DAO.store(vertex1);
			Vertex vertex2 = new Vertex(inLocation, "V02", 1, new Point(PositionTypeEnum.METERS_FROM_PARENT,
				inDimMeters.getX(),
				0.0,
				null));
			Vertex.DAO.store(vertex2);
			Vertex vertex4 = new Vertex(inLocation, "V03", 2, new Point(PositionTypeEnum.METERS_FROM_PARENT,
				inDimMeters.getX(),
				inDimMeters.getY(),
				null));
			Vertex.DAO.store(vertex4);
			Vertex vertex3 = new Vertex(inLocation, "V04", 3, new Point(PositionTypeEnum.METERS_FROM_PARENT,
				0.0,
				inDimMeters.getY(),
				null));
			Vertex.DAO.store(vertex3);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
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

		try {
			CodeshelfNetwork.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final String linkDropbox() {
		String result = "";

		DropboxService dropboxService = this.getDropboxService();

		if (dropboxService != null) {
			result = dropboxService.link();
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Get/assign work instructions for a CHE that's at the listed location with the listed container IDs.
	 * 
	 * Yes, this has high cyclometric complexity, but the creation of a WI in a complex puzzle.  If you decompose this logic into
	 * fractured routines then there's a chance that they could get called out of order or in the wrong order, etc.  Sometimes in life
	 * you have a complex process and there's no way to make it simple.
	 * 
	 * It's critical that this get covered by good quality unit tests as a hedge against breaking stuff in future versions!
	 * That is, if you change this for some logic reason then make sure you add a unit test to capture the reason you've changed it.
	 * 
	 * @param inChe
	 * @param inLocationId
	 * @param inContainerIdList
	 * @return
	 */
	@Transactional
	public final List<WorkInstruction> getWorkInstructions(final Che inChe,
		final String inScannedLocationId,
		final List<String> inContainerIdList) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		ISubLocation<?> cheLocation = findSubLocationById(inScannedLocationId);
		if (cheLocation != null) {
			Aisle aisle = null;
			if (cheLocation instanceof Aisle) {
				aisle = (Aisle) cheLocation;
			} else {
				aisle = cheLocation.<Aisle> getParentAtLevel(Aisle.class);
			}
			PathSegment pathSegment = aisle.getPathSegment();
			Path path = pathSegment.getParent();
			WorkArea workArea = path.getWorkArea();

			if (path != null) {
				// Now figure out the orders that go with these containers.
				List<Container> containerList = new ArrayList<Container>();
				for (String containerId : inContainerIdList) {
					Container container = getContainer(containerId);
					if (container != null) {
						containerList.add(container);
					}
				}

				// Get all of the OUTBOUND work instructions.
				wiResultList.addAll(generateOutboundInstructions(containerList, path, inScannedLocationId, cheLocation));

				// Get all of the CROSS work instructions.
				wiResultList.addAll(generateCrossWallInstructions(containerList, path, inScannedLocationId, cheLocation));

			}
		}
		return wiResultList;
	}

	// --------------------------------------------------------------------------
	/**
	 * Generate pick work instructions for a container at a specific location on a path.
	 * @param inContainerUse
	 * @param inPath
	 * @param inCheLocation
	 * @return
	 */
	private List<WorkInstruction> generateOutboundInstructions(final List<Container> inContainerList,
		final Path inPath,
		final String inScannedLocationId,
		final ISubLocation<?> inCheLocation) {
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
							OrderTypeEnum.OUTBOUND,
							orderDetail,
							0,
							container,
							inScannedLocationId,
							inCheLocation,
							0.0);
						if (plannedWi != null) {
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
							Integer quantityToPick = orderDetail.getQuantity();

							// If there is anything to pick on this item then create a WI for it.
							if (quantityToPick > 0) {
								WorkInstruction plannedWi = createWorkInstruction(WorkInstructionStatusEnum.NEW,
									WorkInstructionTypeEnum.PLAN,
									OrderTypeEnum.OUTBOUND,
									orderDetail,
									quantityToPick,
									container,
									inScannedLocationId,
									foundLocation,
									selectedItem.getPosAlongPath());
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
	private List<WorkInstruction> generateCrossWallInstructions(final List<Container> inContainerList,
		final Path inPath,
		final String inScannedLocationId,
		final ILocation<?> inCheLocation) {

		List<WorkInstruction> wiList = new ArrayList<WorkInstruction>();

		for (Container container : inContainerList) {
			// Iterate over all active CROSS orders on the path.
			OrderHeader crossOrder = container.getCurrentOrderHeader();
			if ((crossOrder.getActive()) && (crossOrder.getOrderTypeEnum().equals(OrderTypeEnum.CROSS))) {
				// Iterate over all active OUTBOUND on the path.
				for (OrderHeader outOrder : getOrderHeaders()) {
					if ((outOrder.getOrderTypeEnum().equals(OrderTypeEnum.OUTBOUND) && (outOrder.getActive()))
							&& (inPath.isOrderOnPath(outOrder))) {
						// OK, we have an OUTBOUND order on the same path as the CROSS order.
						// Check to see if any of the active CROSS order detail items match OUTBOUND order details.
						for (OrderDetail crossOrderDetail : crossOrder.getOrderDetails()) {
							if (crossOrderDetail.getActive()) {
								OrderDetail outOrderDetail = outOrder.getOrderDetail(crossOrderDetail.getOrderDetailId());
								if ((outOrderDetail != null) && (outOrderDetail.getActive())) {

									OrderLocation firstOutOrderLoc = outOrder.getFirstOrderLocationOnPath(inPath);
									// Now make sure
									// The outboundOrder is "ahead" of the CHE's position on the path.
									// The UOM matches.
									if ((firstOutOrderLoc.getLocation().getPosAlongPath() > inCheLocation.getPosAlongPath())
											&& (outOrderDetail.getUomMasterId().equals(crossOrderDetail.getUomMasterId()))) {

										WorkInstruction wi = createWorkInstruction(WorkInstructionStatusEnum.NEW,
											WorkInstructionTypeEnum.PLAN,
											OrderTypeEnum.CROSS,
											outOrderDetail,
											outOrderDetail.getQuantity(),
											container,
											inScannedLocationId,
											firstOutOrderLoc.getLocation(),
											firstOutOrderLoc.getLocation().getPosAlongPath());

										// If we created a WI then add it to the list.
										if (wi != null) {
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

		// Now we need to sort and group the work instructions, so that the CHE can display them by working order.
		List<Bay> bays = inPath.<Bay> getLocationsByClassAtOrPastLocation(inCheLocation, Bay.class);
		return sortCrosswallInstructions(wiList, inContainerList, bays);
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
	 * @param inContainerList
	 * @param inBays
	 * @return
	 */
	private List<WorkInstruction> sortCrosswallInstructions(final List<WorkInstruction> inCrosswallWiList,
		final List<Container> inContainerList,
		final List<Bay> inBays) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		// We want to work all of the WIs out of a single container for each bay.
		// To do this, we sort the WIs by item ID and then work the list bay-by-bay, container-by-container.
		Collections.sort(inCrosswallWiList, new WiItemIdComparator());

		// Cycle over all bays on the path.
		for (Bay bay : inBays) {
			// Cycle over all of the containers until we find no more work instructions for this bay.
			while (true) {
				boolean wiSelected = false;
				for (Container container : inContainerList) {
					Iterator<WorkInstruction> wiIterator = inCrosswallWiList.iterator();
					while (wiIterator.hasNext()) {
						WorkInstruction wi = wiIterator.next();
						if (wi.getContainer().equals(container)) {
							if (wi.isContainedByLocation(bay)) {
								String wantedItemId = wi.getItemMaster().getItemId();
								wiResultList.add(wi);
								wi.setGroupAndSortCode(String.format("%04d", wiResultList.size()));
								WorkInstruction.DAO.store(wi);
								wiSelected = true;
								wiIterator.remove();
								// Keep moving through the WI list for more WIs to consider.
								while (wiIterator.hasNext()) {
									wi = wiIterator.next();
									// No more WIs with matching item type, so break.
									if (!wi.getItemMaster().getItemId().equals(wantedItemId)) {
										break;
									} else {
										// No more WIs in this bay, so break.
										if (!wi.isContainedByLocation(bay)) {
											break;
										} else {
											// Found another matching WI to add for this bay.
											wiResultList.add(wi);
											wiIterator.remove();
											wi.setGroupAndSortCode(String.format("%04d", wiResultList.size()));
											WorkInstruction.DAO.store(wi);
										}
									}
								}

							}
						}
					}
				}
				// If we didn't select any WIs then stop looking for items in containers for this bay.
				if (!wiSelected) {
					break;
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
		final OrderTypeEnum inOrderType,
		OrderDetail inOrderDetail,
		Integer inQuantityToPick,
		Container inContainer,
		String inScannedLocationId,
		ISubLocation<?> inLocation,
		Double inPosALongPath) {
		WorkInstruction resultWi = null;

		for (WorkInstruction wi : inOrderDetail.getWorkInstructions()) {
			if (wi.getTypeEnum().equals(WorkInstructionTypeEnum.PLAN)) {
				resultWi = wi;
				break;
			} else if (wi.getTypeEnum().equals(WorkInstructionTypeEnum.ACTUAL)) {
				// Deduct any WIs already completed for this line item.
				inQuantityToPick -= wi.getActualQuantity();
			}
		}

		// Check if there is any left to pick.
		if (inQuantityToPick > 0) {

			// If there is no planned WI then create one.
			if (resultWi == null) {
				resultWi = new WorkInstruction();
				resultWi.setParent(inOrderDetail);
				resultWi.setCreated(new Timestamp(System.currentTimeMillis()));
			}

			// Set the LED lighting pattern for this WI.
			setWorkInstructionLedPattern(resultWi, inOrderType, inOrderDetail.getItemMasterId(), inLocation);

			// Update the WI
			resultWi.setDomainId(Long.toString(System.currentTimeMillis()));
			resultWi.setTypeEnum(inType);
			resultWi.setStatusEnum(inStatus);

			resultWi.setLocation(inLocation);
			resultWi.setLocationId(inLocation.getFullDomainId());
			resultWi.setItemMaster(inOrderDetail.getItemMaster());
			resultWi.setDescription(inOrderDetail.getItemMaster().getDescription());
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
			resultWi.setPosAlongPath(inPosALongPath);
			resultWi.setContainer(inContainer);
			resultWi.setPlanQuantity(inQuantityToPick);
			resultWi.setActualQuantity(0);
			resultWi.setAssigned(new Timestamp(System.currentTimeMillis()));
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
	 * Create the LED lighting pattern for the WI.
	 * @param inWi
	 * @param inOrderType
	 * @param inItemId
	 * @param inLocation
	 */
	private void setWorkInstructionLedPattern(final WorkInstruction inWi,
		final OrderTypeEnum inOrderType,
		final String inItemId,
		final ISubLocation<?> inLocation) {

		// Determine the first and last LED positions for this instruction.
		short firstLedPosNum = 0;
		short lastLedPosNum = 0;
		if (inOrderType.equals(OrderTypeEnum.OUTBOUND)) {
			firstLedPosNum = inLocation.getFirstLedPosForItemId(inItemId);
			lastLedPosNum = inLocation.getLastLedPosForItemId(inItemId);
		} else if (inOrderType.equals(OrderTypeEnum.CROSS)) {
			firstLedPosNum = inLocation.getFirstLedNumAlongPath();
			lastLedPosNum = inLocation.getLastLedNumAlongPath();
		}

		// Put the positions into increasing order.
		if (firstLedPosNum > lastLedPosNum) {
			Short temp = firstLedPosNum;
			firstLedPosNum = lastLedPosNum;
			lastLedPosNum = temp;
		}

		// The new way of sending LED data to the remote controller.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();
		LedCmdGroup ledCmdGroup = new LedCmdGroup(inLocation.getLedController().getDeviceGuidStr(),
			inLocation.getLedChannel(),
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
}
