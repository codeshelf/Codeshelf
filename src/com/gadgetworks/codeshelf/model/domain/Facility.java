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
@CacheStrategy(useBeanCache = true)
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
	//@Getter
	private Map<String, Container>			containers		= new HashMap<String, Container>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
	@MapKey(name = "domainId")
	@Getter
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
	@Getter
	private Map<String, Path>				paths			= new HashMap<String, Path>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, UomMaster>			uomMasters		= new HashMap<String, UomMaster>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, LocationAlias>		locationAliases	= new HashMap<String, LocationAlias>();

	public Facility() {

	}

	public Facility(final Double inPosX, final Double inPosY) {
		super(PositionTypeEnum.GPS, inPosX, inPosY);
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

	public final List<LocationAlias> getLocationAliases() {
		return new ArrayList<LocationAlias>(locationAliases.values());
	}

	public final void removeLocationAlias(String inLocationAliasId) {
		locationAliases.remove(inLocationAliasId);
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
		final Boolean inOpensLowSide) {

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
				aisle = new Aisle(this, inAisleId, inPosXMeters, inPosYMeters);
				try {
					Aisle.DAO.store(aisle);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}

				Double anchorPosX = 0.0;
				Double anchorPosY = 0.0;
				Double aisleBoundaryX = 0.0;
				Double aisleBoundaryY = 0.0;

				Short curLedPosNum = 1;
				Short channelNum = 1;
				Boolean tiersTopToBottom = false;
				for (int bayNum = 1; bayNum <= inBaysLong; bayNum++) {
					Double anchorPosZ = 0.0;
					for (int bayHighNum = 0; bayHighNum < inBaysHigh; bayHighNum++) {
						String bayName = "B" + bayNum + "." + bayHighNum;
						Bay bay = createZigZagBay(aisle,
							bayName,
							curLedPosNum,
							tiersTopToBottom,
							anchorPosX,
							anchorPosY,
							anchorPosZ,
							inRunInXDir,
							ledController,
							channelNum);
						aisle.addLocation(bay);

						tiersTopToBottom = !tiersTopToBottom;

						// Get the last LED position from the bay to setup the next one.
						curLedPosNum = (short) (bay.getLastLedNumAlongPath());

						// Create the bay's boundary vertices.
						if (inRunInXDir) {
							createVertices(bay, inProtoBayWidthMeters, inProtoBayDepthMeters);
						} else {
							createVertices(bay, inProtoBayDepthMeters, inProtoBayWidthMeters);
						}

						anchorPosZ += inProtoBayHeightMeters;
					}

					// Prepare the anchor point for the next bay.
					if (inRunInXDir) {
						if ((anchorPosX + inProtoBayWidthMeters) > aisleBoundaryX) {
							aisleBoundaryX = anchorPosX + inProtoBayWidthMeters;
						}

						if ((anchorPosY + inProtoBayDepthMeters) > aisleBoundaryY) {
							aisleBoundaryY = anchorPosY + inProtoBayDepthMeters;
						}

						anchorPosX += inProtoBayWidthMeters;
					} else {
						if ((anchorPosX + inProtoBayDepthMeters) > aisleBoundaryX) {
							aisleBoundaryX = anchorPosX + inProtoBayDepthMeters;
						}

						if ((anchorPosY + inProtoBayWidthMeters) > aisleBoundaryY) {
							aisleBoundaryY = anchorPosY + inProtoBayWidthMeters;
						}

						anchorPosY += inProtoBayDepthMeters;
					}
				}

				// Create the aisle's boundary vertices.
				createVertices(aisle, aisleBoundaryX, aisleBoundaryY);

				// Create the paths related to this aisle.
				aisle.createPaths(aisleBoundaryX, aisleBoundaryY, TravelDirectionEnum.FORWARD, inOpensLowSide);

			}
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
		final Short inFirstLedNum,
		final Boolean inTierTopToBottom,
		final Double inAnchorPosX,
		final Double inAnchorPosY,
		final Double inAnchorPosZ,
		final Boolean inRunsInXDir,
		final LedController inLedController,
		final short inLedChannelNum) {

		Bay resultBay = null;

		resultBay = new Bay(inParentAisle, inBayId, inAnchorPosX, inAnchorPosY, inAnchorPosZ);

		resultBay.setFirstLedNumAlongPath(inFirstLedNum);
		resultBay.setLastLedNumAlongPath((short) (inFirstLedNum + 160));

		try {
			Bay.DAO.store(resultBay);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		// DEMOWARE - this creates the bays we have in the office for demos.
		// We need to create the UI to setup these bays properly and get rid of this hard-coding.

		// Add slots to this tier.
		if (inTierTopToBottom) {
			createTier(resultBay, "T1", true, inRunsInXDir, 0.0, 0.0, inLedController, inLedChannelNum, (short) 129, (short) 160);
			createTier(resultBay, "T2", false, inRunsInXDir, 0.0, 0.25, inLedController, inLedChannelNum, (short) 128, (short) 97);
			createTier(resultBay, "T3", true, inRunsInXDir, 0.0, 0.5, inLedController, inLedChannelNum, (short) 65, (short) 96);
			createTier(resultBay, "T4", false, inRunsInXDir, 0.0, 1.0, inLedController, inLedChannelNum, (short) 64, (short) 33);
			createTier(resultBay, "T5", true, inRunsInXDir, 0.0, 1.25, inLedController, inLedChannelNum, (short) 1, (short) 32);
		} else {
			createTier(resultBay, "T1", true, inRunsInXDir, 0.0, 0.0, inLedController, inLedChannelNum, (short) 1, (short) 32);
			createTier(resultBay, "T2", false, inRunsInXDir, 0.0, 0.25, inLedController, inLedChannelNum, (short) 64, (short) 33);
			createTier(resultBay, "T3", true, inRunsInXDir, 0.0, 0.5, inLedController, inLedChannelNum, (short) 65, (short) 96);
			createTier(resultBay, "T4", false, inRunsInXDir, 0.0, 1.0, inLedController, inLedChannelNum, (short) 128, (short) 97);
			createTier(resultBay, "T5", true, inRunsInXDir, 0.0, 1.25, inLedController, inLedChannelNum, (short) 129, (short) 160);
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
	 * @param inSlotLeftToRight
	 * @param inRunsInXDir
	 * @param inOffset1
	 * @param inOffset2
	 * @param inLedController
	 * @param inLedChannelNum
	 */
	private void createTier(final Bay inParentBay,
		final String inTierId,
		final Boolean inSlotLeftToRight,
		final Boolean inRunsInXDir,
		final Double inOffset1,
		final Double inOffset2,
		final LedController inLedController,
		final Short inLedChannelNum,
		final Short inFirstLedPosNum,
		final Short inLastLedPosNum) {

		Tier tier = null;
		if (inRunsInXDir) {
			tier = new Tier(inOffset1, inOffset2);
		} else {
			tier = new Tier(inOffset2, inOffset1);
		}

		tier.setDomainId(inTierId);
		tier.setLedController(inLedController);
		tier.setLedChannel(inLedChannelNum);
		tier.setFirstLedNumAlongPath((short) (inParentBay.getFirstLedNumAlongPath() + inFirstLedPosNum - 1));
		tier.setLastLedNumAlongPath((short) (inParentBay.getFirstLedNumAlongPath() + inLastLedPosNum - 1));
		tier.setParent(inParentBay);
		inParentBay.addLocation(tier);
		try {
			Tier.DAO.store(tier);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		// Add slots to this tier.
		if (inSlotLeftToRight) {
			createSlot(tier, "S1", inRunsInXDir, 0.0, 0.0, inLedController, inLedChannelNum, (short) 2, (short) 9);
			createSlot(tier, "S2", inRunsInXDir, 0.25, 0.0, inLedController, inLedChannelNum, (short) 11, (short) 18);
			createSlot(tier, "S3", inRunsInXDir, 0.5, 0.0, inLedController, inLedChannelNum, (short) 20, (short) 26);
			createSlot(tier, "S4", inRunsInXDir, 1.0, 0.0, inLedController, inLedChannelNum, (short) 28, (short) 32);
		} else {
			createSlot(tier, "S1", inRunsInXDir, 0.0, 0.0, inLedController, inLedChannelNum, (short) 31, (short) 24);
			createSlot(tier, "S2", inRunsInXDir, 0.25, 0.0, inLedController, inLedChannelNum, (short) 22, (short) 15);
			createSlot(tier, "S3", inRunsInXDir, 0.5, 0.0, inLedController, inLedChannelNum, (short) 13, (short) 6);
			createSlot(tier, "S4", inRunsInXDir, 1.0, 0.0, inLedController, inLedChannelNum, (short) 4, (short) 1);
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
		final Double inOffset1,
		final Double inOffset2,
		final LedController inLedController,
		final Short inChannelNum,
		final Short inFirstLedPosNum,
		final Short inLastLedPosNum) {

		Slot slot = null;
		if (inRunsInXDir) {
			slot = new Slot(inOffset1, inOffset2);
		} else {
			slot = new Slot(inOffset2, inOffset1);
		}

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
	private void createVertices(ILocation inLocation, Double inXDimMeters, Double inYDimMeters) {
		try {
			// Create four simple vertices around the aisle.
			Vertex vertex1 = new Vertex(inLocation, "V01", 0, new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, null));
			Vertex.DAO.store(vertex1);
			Vertex vertex2 = new Vertex(inLocation, "V02", 1, new Point(PositionTypeEnum.METERS_FROM_PARENT,
				inXDimMeters,
				0.0,
				null));
			Vertex.DAO.store(vertex2);
			Vertex vertex4 = new Vertex(inLocation, "V03", 2, new Point(PositionTypeEnum.METERS_FROM_PARENT,
				inXDimMeters,
				inYDimMeters,
				null));
			Vertex.DAO.store(vertex4);
			Vertex vertex3 = new Vertex(inLocation, "V04", 3, new Point(PositionTypeEnum.METERS_FROM_PARENT,
				0.0,
				inYDimMeters,
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
		final String inLocationId,
		final List<String> inContainerIdList) {
		
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		ILocation<?> cheLocation = findSubLocationById(inLocationId);
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
				for (String containerId : inContainerIdList) {
					Container container = getContainer(containerId);
					if (container != null) {
						// Find the container use with the latest timestamp - that's the active one.
						Timestamp timestamp = null;
						ContainerUse foundContainerUse = null;
						for (ContainerUse use : container.getUses()) {
							if ((timestamp == null) || (use.getUsedOn().after(timestamp))) {
								timestamp = use.getUsedOn();
								foundContainerUse = use;
							}
						}
						if (foundContainerUse != null) {
							OrderHeader order = foundContainerUse.getOrderHeader();
							if (order != null) {
								if (order.getOrderTypeEnum().equals(OrderTypeEnum.OUTBOUND)) {
									wiResultList.addAll(generateOutboundInstructions(foundContainerUse, order, path, cheLocation));
								} else if (order.getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) {
									wiResultList.addAll(generateCrossWallInstructions(foundContainerUse, order, path, cheLocation));
								}
							}
						}
					}
				}

				// If we found WIs then sort them by they distance from the named location (closest first).
				if (wiResultList.size() > 0) {
					path.sortWisByDistance(wiResultList);
				}
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
	private List<WorkInstruction> generateOutboundInstructions(final ContainerUse inContainerUse,
		final OrderHeader inOrder,
		final Path inPath,
		final ILocation<?> inCheLocation) {
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		for (OrderDetail orderDetail : inOrder.getOrderDetails()) {
			ItemMaster itemMaster = orderDetail.getItemMaster();

			// Figure out if there are any items are on the current path.
			// (We just take the first one we find, because items slotted on the same path should be close together.)
			Item selectedItem = null;

			if (itemMaster.getItems().size() == 0) {
				// If there is no item in inventory (AT ALL) then create a PLANEED, SHORT WI for this order detail.
				WorkInstruction plannedWi = createWorkInstruction(WorkInstructionStatusEnum.SHORT,
					WorkInstructionTypeEnum.ACTUAL,
					orderDetail,
					0,
					inContainerUse.getParentContainer(),
					(ISubLocation<IDomainObject>) inCheLocation,
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
							orderDetail,
							quantityToPick,
							inContainerUse.getParentContainer(),
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

						inOrder.setStatusEnum(OrderStatusEnum.INPROGRESS);
						try {
							OrderHeader.DAO.store(inOrder);
						} catch (DaoException e) {
							LOGGER.error("", e);
						}
					}
				}
			}
		}

		return wiResultList;
	}

	// --------------------------------------------------------------------------
	/**
	 * Find all of the OUTBOUND orders that need items on this CROSS order.
	 * @param inContainerUse
	 * @param inOrder
	 * @param inPath
	 * @param inCheLocation
	 * @return
	 */
	private List<WorkInstruction> generateCrossWallInstructions(final ContainerUse inContainerUse,
		final OrderHeader inCrossOrder,
		final Path inPath,
		final ILocation<?> inCheLocation) {
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		for (OrderHeader outboundOrder : getOrderHeaders()) {
			if (outboundOrder.getOrderTypeEnum().equals(OrderTypeEnum.OUTBOUND)) {
				// Determine if this OUTBOUND order is on the same path as the CROSS order.
				for (OrderLocation outboundOrderLocation : outboundOrder.getOrderLocations()) {
					if (inPath.isLocationOnPath(outboundOrderLocation.getLocation())) {
						// See if the any of the outbound order details items match the cross order details.
						for (OrderDetail outboundOrderDetail : outboundOrder.getOrderDetails()) {
							for (OrderDetail crossOrderDetail : inCrossOrder.getOrderDetails()) {
								if (outboundOrderDetail.getItemMasterId().equals(crossOrderDetail.getItemMasterId())) {
									ISubLocation<IDomainObject> foundLocation = (ISubLocation<IDomainObject>) inCheLocation;
									WorkInstruction wi = createWorkInstruction(WorkInstructionStatusEnum.NEW,
										WorkInstructionTypeEnum.PLAN,
										crossOrderDetail,
										crossOrderDetail.getQuantity(),
										inContainerUse.getParentContainer(),
										foundLocation,
										outboundOrderLocation.getLocation().getPosAlongPath());
									wiResultList.add(wi);
								}
							}
						}
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
		Integer inQuantityToPick,
		Container inContainer,
		ISubLocation<IDomainObject> inLocation,
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

			// Update the WI
			resultWi.setDomainId(Long.toString(System.currentTimeMillis()));
			resultWi.setTypeEnum(inType);
			resultWi.setStatusEnum(inStatus);

			// The old way of sending LED data to the remote controller.
			//			resultWi.setLedControllerId(inLocation.getLedController().getDeviceGuidStr());
			//			resultWi.setLedChannel(inLocation.getLedChannel());
			//			resultWi.setLedFirstPos(inLocation.getFirstLedPosForItemId(inOrderDetail.getItemMaster().getItemId()));
			//			resultWi.setLedLastPos(inLocation.getLastLedPosForItemId(inOrderDetail.getItemMaster().getItemId()));
			//			resultWi.setLedColorEnum(ColorEnum.BLUE);

			// Add all of the LEDs we have to light to make this work.
			short firstLedPosNum = inLocation.getFirstLedPosForItemId(inOrderDetail.getItemMaster().getItemId());
			short lastLedPosNum = inLocation.getLastLedPosForItemId(inOrderDetail.getItemMaster().getItemId());

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
			resultWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));

			//resultWi.setLocationId(((ISubLocation<?>) inLocation.getParent()).getLocationId() + "." + inLocation.getLocationId());
			resultWi.setLocationId(inLocation.getFullDomainId());
			resultWi.setItemId(inOrderDetail.getItemMaster().getItemId());
			resultWi.setDescription(inOrderDetail.getItemMaster().getDescription());
			if (inOrderDetail.getItemMaster().getDdcId() != null) {
				resultWi.setPickInstruction(inOrderDetail.getItemMaster().getDdcId());
			} else {
				// TODO: create a "getLocationIdToLevel(<Location Class>);"
				Bay bay = inLocation.getParentAtLevel(Bay.class);
				Tier tier = inLocation.getParentAtLevel(Tier.class);
				resultWi.setPickInstruction(bay.getLocationId() + "." + tier.getLocationId() + "." + inLocation.getLocationId());
			}
			resultWi.setPosAlongPath(inPosALongPath);
			resultWi.setContainerId(inContainer.getContainerId());
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

	private class DdcItemComparator implements Comparator<Item> {

		public int compare(Item inItem1, Item inItem2) {
			return inItem1.getParent().getDdcId().compareTo(inItem2.getParent().getDdcId());
		}
	};

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

		// Make a list of all locations that have a DDC start/end.
		LOGGER.debug("DDC get locations");
		List<ILocation> ddcLocations = new ArrayList<ILocation>();
		for (Aisle aisle : getAisles()) {
			for (ILocation location : aisle.getChildren()) {
				if (location.getFirstDdcId() != null) {
					ddcLocations.add(location);
				}
			}
		}

		// Loop through all of the DDC items in the facility.
		LOGGER.debug("DDC get items");
		List<ItemMaster> ddcItemMasters = new ArrayList<ItemMaster>();
		for (ItemMaster itemMaster : getItemMasters()) {
			if ((itemMaster.getDdcId() != null) && (itemMaster.getActive())) {
				ddcItemMasters.add(itemMaster);
			}
		}

		// Sort the DDC items in lex/DDC order.
		LOGGER.debug("DDC sort items");
		Collections.sort(ddcItemMasters, new DdcItemMasterComparator());

		// Get the items that belong to each DDC location.
		LOGGER.debug("DDC list items");
		List<Item> locationItems = new ArrayList<Item>();
		Double locationItemCount;
		for (ILocation<?> location : ddcLocations) {
			// Delete all of the old DDC groups from this location.
			for (ItemDdcGroup ddcGroup : location.getDdcGroups()) {
				ItemDdcGroup.DAO.delete(ddcGroup);
			}

			// Build a list of all items in this DDC-based location.
			LOGGER.debug("DDC location check: " + location.getFullDomainId() + " " + location.getPersistentId());
			locationItems.clear();
			locationItemCount = 0.0;
			for (ItemMaster itemMaster : ddcItemMasters) {
				if ((itemMaster.getDdcId().compareTo(location.getFirstDdcId()) >= 0)
						&& (itemMaster.getDdcId().compareTo(location.getLastDdcId()) <= 0)) {
					for (Item item : itemMaster.getItems()) {
						LOGGER.debug("DDC assign item: " + "loc: " + location.getFullDomainId() + " itemId: "
								+ itemMaster.getItemId() + " Ddc: " + item.getParent().getDdcId());
						item.setStoredLocation(location);
						location.addItem(item);
						locationItems.add(item);
						locationItemCount += item.getQuantity();
					}
				}
			}

			// Compute the length of the location's face.
			Double locationLen = 0.0;
			Vertex lastVertex = null;
			List<Vertex> list = location.getVertices();
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

			// Walk through all of the items in this location in DDC order and position them.
			Double ddcPos = location.getPosAlongPath();
			Double distPerItem = locationLen / locationItemCount;
			ItemDdcGroup lastDdcGroup = null;
			Collections.sort(locationItems, new DdcItemComparator());
			for (Item item : locationItems) {
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

		LOGGER.debug("End DDC position recompute");

	}
}
