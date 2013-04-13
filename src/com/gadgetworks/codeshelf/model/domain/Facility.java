/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.65 2013/04/13 02:26:29 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.ColorEnum;
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
//@ToString
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
	@Getter
	private Map<String, Container>			containers		= new HashMap<String, Container>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, ContainerKind>		containerKinds	= new HashMap<String, ContainerKind>();

	@OneToMany(mappedBy = "parent", targetEntity = DropboxService.class)
	@Getter
	private List<IEdiService>				ediServices		= new ArrayList<IEdiService>();

	@OneToMany(mappedBy = "parent")
	@Getter
	private List<ItemMaster>				itemMasters		= new ArrayList<ItemMaster>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, CodeshelfNetwork>	networks		= new HashMap<String, CodeshelfNetwork>();

	@OneToMany(mappedBy = "parent")
	@Getter
	private List<OrderGroup>				orderGroups		= new ArrayList<OrderGroup>();

	@OneToMany(mappedBy = "parent")
	@Getter
	private List<OrderHeader>				orderHeaders	= new ArrayList<OrderHeader>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, Path>				paths			= new HashMap<String, Path>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@MapKey(name = "domainId")
	@Getter
	private Map<String, UomMaster>			uomMasters		= new HashMap<String, UomMaster>();

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

	public final void addOrderHeader(OrderHeader inOrderHeader) {
		orderHeaders.add(inOrderHeader);
	}

	public final void removeOrderHeader(OrderHeader inOrderHeaders) {
		orderHeaders.remove(inOrderHeaders);
	}

	public final void addOrderGroup(OrderGroup inOrderGroup) {
		orderGroups.add(inOrderGroup);
	}

	public final void removeOrderGroup(OrderGroup inOrderGroups) {
		orderGroups.remove(inOrderGroups);
	}

	public final void addItemMaster(ItemMaster inItemMaster) {
		itemMasters.add(inItemMaster);
	}

	public final void removeItemMaster(ItemMaster inItemMasters) {
		itemMasters.remove(inItemMasters);
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

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderID
	 * @return
	 */
	public final OrderGroup findOrderGroup(String inOrderGroupID) {
		OrderGroup result = null;

		for (OrderGroup orderGroup : getOrderGroups()) {
			if (orderGroup.getDomainId().equals(inOrderGroupID)) {
				result = orderGroup;
				break;
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderID
	 * @return
	 */
	public final OrderHeader findOrder(String inOrderID) {
		OrderHeader result = null;

		for (OrderHeader order : getOrderHeaders()) {
			if (order.getDomainId().equals(inOrderID)) {
				result = order;
				break;
			}
		}

		return result;
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
		final Double inProtoBayXDimMeters,
		final Double inProtoBayYDimMeters,
		final Double inProtoBayZDimMeters,
		final Integer inBaysHigh,
		final Integer inBaysLong,
		final Boolean inRunInXDir,
		final Boolean inOpensLowSide) {

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

			int bayNum = 0;
			for (int bayLongNum = 0; bayLongNum < inBaysLong; bayLongNum++) {
				Double anchorPosZ = 0.0;
				for (int bayHighNum = 0; bayHighNum < inBaysHigh; bayHighNum++) {
					String bayId = String.format("%0" + Integer.toString(getIdDigits()) + "d", bayNum++);
					Bay bay = new Bay(aisle, bayId, anchorPosX, anchorPosY, anchorPosZ);
					try {
						Bay.DAO.store(bay);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
					aisle.addLocation(bay);

					// Create the bay's boundary vertices.
					createVertices(bay, inProtoBayXDimMeters, inProtoBayYDimMeters);

					anchorPosZ += inProtoBayZDimMeters;
				}

				if ((anchorPosX + inProtoBayXDimMeters) > aisleBoundaryX) {
					aisleBoundaryX = anchorPosX + inProtoBayXDimMeters;
				}

				if ((anchorPosY + inProtoBayYDimMeters) > aisleBoundaryY) {
					aisleBoundaryY = anchorPosY + inProtoBayYDimMeters;
				}

				// Prepare the anchor point for the next bay.
				if (inRunInXDir) {
					anchorPosX += inProtoBayXDimMeters;
				} else {
					anchorPosY += inProtoBayYDimMeters;
				}
			}

			// Create the aisle's boundary vertices.
			createVertices(aisle, aisleBoundaryX, aisleBoundaryY);

			// Create the paths related to this aisle.
			aisle.createPaths(aisleBoundaryX, aisleBoundaryY, TravelDirectionEnum.FORWARD, inOpensLowSide);

			// Create at least one aisle controller.
			CodeshelfNetwork network = networks.get(CodeshelfNetwork.DEFAULT_NETWORK_ID);
			if (network != null) {
				network.createLedController("0x00000000");
			}
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
							LOGGER.info("Location: " + bay.getFullDomainId() + " is " + bay.getPathDistance()
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
	 * @param inChe
	 * @param inLocationId
	 * @param inContainerIdList
	 * @return
	 */
	@Transactional
	public final List<WorkInstruction> getWorkInstructions(final Che inChe,
		final String inLocationId,
		final List<String> inContainerIdList) {
		List<WorkInstruction> result = new ArrayList<WorkInstruction>();

		ILocation<?> cheLocation = getSubLocationById(inLocationId);
		Aisle aisle = cheLocation.<Aisle> getParentAtLevel(Aisle.class);
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
					ContainerUse foundUse = null;
					for (ContainerUse use : container.getUses()) {
						if ((timestamp == null) || (use.getUsedOn().after(timestamp))) {
							timestamp = use.getUsedOn();
							foundUse = use;
						}
					}
					if (foundUse != null) {
						OrderHeader order = foundUse.getOrderHeader();
						if (order != null) {
							for (OrderDetail detail : order.getOrderDetails()) {
								ISubLocation<IDomainObject> foundLocation = null;
								String parentLocationId = "";
								ItemMaster itemMaster = detail.getItemMaster();

								// Figure out if there are any items are on the current path.
								// (We just take the first one we find, because items slotted on the same path should be close together.)
								for (Item item : itemMaster.getItems()) {
									ISubLocation location = (ISubLocation) item.getParent();
									if (path.isLocationOnPath(location)) {
										foundLocation = location;
										parentLocationId = ((ISubLocation<?>) foundLocation.getParent()).getLocationId();
										break;
									}
								}

								// The item is on the CHE's path, so add it.
								if (foundLocation != null) {
									Integer quantityToPick = detail.getQuantity();
									WorkInstruction plannedWi = null;
									for (WorkInstruction wi : detail.getWorkInstructions()) {
										if (wi.getTypeEnum().equals(WorkInstructionTypeEnum.PLAN)) {
											plannedWi = wi;
										} else if (wi.getTypeEnum().equals(WorkInstructionTypeEnum.ACTUAL)) {
											// Deduct any WIs alreadty completed for this line item.
											quantityToPick -= wi.getPlanQuantity();
										}
									}

									// If there is anything to pick on this item then create a WI for it.
									if (quantityToPick > 0) {
										// If there is no planned WI then create one.
										if (plannedWi == null) {
											plannedWi = new WorkInstruction();
											plannedWi.setParent(detail);
											plannedWi.setCreated(new Timestamp(System.currentTimeMillis()));
										}

										// Update the WI
										plannedWi.setDomainId(order.getOrderId() + "." + detail.getOrderDetailId());
										plannedWi.setTypeEnum(WorkInstructionTypeEnum.PLAN);
										plannedWi.setStatusEnum(WorkInstructionStatusEnum.NEW);
										plannedWi.setLedControllerCommand("");
										plannedWi.setLedControllerId("0x00000003");
										plannedWi.setColorEnum(ColorEnum.BLUE);
										plannedWi.setItemId(itemMaster.getItemId());
										plannedWi.setLocationId(parentLocationId + "." + foundLocation.getLocationId());
										plannedWi.setContainerId(containerId);
										plannedWi.setPlanQuantity(quantityToPick);
										plannedWi.setAssigned(new Timestamp(System.currentTimeMillis()));
										try {
											WorkInstruction.DAO.store(plannedWi);
										} catch (DaoException e) {
											LOGGER.error("", e);
										}

										result.add(plannedWi);

										detail.setStatusEnum(OrderStatusEnum.INPROGRESS);
										try {
											OrderDetail.DAO.store(detail);
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
			}

			// If we found WIs then sort them by they distance from the named location (closest first).
			if (result.size() > 0) {
				path.sortWisByDistance(result);
			}
		}
		return result;
	}

	/**
	 * Class to compare items by their DDC.
	 *
	 */
	private class DdcComparator implements Comparator<ItemMaster> {

		public int compare(ItemMaster inItemMaster1, ItemMaster inItemMaster2) {
			return inItemMaster1.getDdcId().compareTo(inItemMaster2.getDdcId());
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * After a change in DDC items we call this routine to recompute the positions of the items.
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
		Collections.sort(ddcItemMasters, new DdcComparator());

		// Get the items that belong to each DDC location.
		LOGGER.debug("DDC list items");
		for (ILocation location : ddcLocations) {
			LOGGER.debug("DDC location check: " + location.getFullDomainId() + " " + location.getPersistentId());
			for (ItemMaster itemMaster : ddcItemMasters) {
				if ((itemMaster.getDdcId().compareTo(location.getFirstDdcId()) >= 0)
						&& (itemMaster.getDdcId().compareTo(location.getLastDdcId()) <= 0)) {
					for (Item item : itemMaster.getItems()) {
						LOGGER.debug("DDC assign item: " + item.getItemId() + " Ddc: " + item.getItemMaster().getDdcId());
						item.setParent(location);
						try {
							item.DAO.store(item);
						} catch (DaoException e) {
							LOGGER.error("", e);
						}
					}
				}
			}
		}

		LOGGER.debug("End DDC position recompute");

	}
}
