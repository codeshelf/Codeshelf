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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.annotation.Transactional;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.InventorySlottedCsvBean;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerABC;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerFactory;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerType;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.Errors;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.base.Strings;
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
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
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

	private static final Logger				LOGGER			= LoggerFactory.getLogger(Facility.class);

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
	private List<Aisle>						aisles			= new ArrayList<Aisle>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, Container>			containers		= new HashMap<String, Container>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
	@MapKey(name = "domainId")
	private Map<String, ContainerKind>		containerKinds	= new HashMap<String, ContainerKind>();

	@OneToMany(mappedBy = "parent", targetEntity = EdiServiceABC.class)
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

	@Transient
	// for now installation specific.  property needs to be exposed as a configuration parameter.
	@Getter
	@Setter
	static WorkInstructionSequencerType		sequencerType	= WorkInstructionSequencerType.BayDistance;

	static {
		String sequencerConfig = System.getProperty("facility.sequencer");
		if ("BayDistance".equalsIgnoreCase(sequencerConfig)) {
			sequencerType = WorkInstructionSequencerType.BayDistance;
		} else if ("BayDistanceTopLast".equalsIgnoreCase(sequencerConfig)) {
			sequencerType = WorkInstructionSequencerType.BayDistanceTopLast;
		}
	}

	public Facility() {
		super(null, null, Point.getZeroPoint(), Point.getZeroPoint());
	}

	public Facility(Organization organization, String domainId, final Point inAnchorPoint) {
		super(null, domainId, inAnchorPoint, Point.getZeroPoint());
		setParentOrganization(organization);
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
	public final Path createPath(String inDomainId, PathSegment[] inPathSegments) {
		Path path = createPath(inDomainId);
		for (PathSegment pathSegment : inPathSegments) {
			pathSegment.setParent(path);
			PathSegment.DAO.store(pathSegment);
			path.addPathSegment(pathSegment);
		}
		return path;
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
	public final void ensureIronMqService() {
		// This is a weak kludge. Just do the get, which does a get and create if not found.
		// Otherwise, the create only happens upon the first attempt at a work instruction save.
		IronMqService theService = getIronMqService();
		if (theService == null)
			LOGGER.error("Failed to get IronMQ service");
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
			LOGGER.info("Creating IronMQ service");
			try {
				return createIronMqService();
			} catch (PSQLException e) {
				LOGGER.error("SQL error trying to create IronMqService", e);
				// allow it to return null
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final IronMqService createIronMqService() throws PSQLException {
		// we saw the PSQL exception in staging test when the record could not be added
		IronMqService result = null;

		result = new IronMqService();
		result.setParent(this);
		result.setDomainId("IRONMQ");
		result.setProviderEnum(EdiProviderEnum.IRONMQ);
		result.setServiceStateEnum(EdiServiceStateEnum.UNLINKED);
		result.setCredentials("", ""); // non-null credentials
		this.addEdiService(result);
		try {
			IronMqService.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("Failed to save IronMQ service", e);
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

		// Work around serious ebeans problem. See OrderHeader's orderDetails cache getting trimmed and then failing to get work instructions made for some orders.
		OrderHeader.DAO.clearAllCaches();

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		//manually track changed ches here to trigger an update broadcast
		Set<Che> changedChes = new HashSet<Che>();
		changedChes.add(inChe);

		// Delete any planned WIs for this CHE.
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("chePersistentId", inChe.getPersistentId().toString());
		filterParams.put("type", WorkInstructionTypeEnum.PLAN.toString());
		for (WorkInstruction wi : WorkInstruction.DAO.findByFilter("assignedChe.persistentId = :chePersistentId and typeEnum = :type",
			filterParams)) {
			try {

				Che assignedChe = wi.getAssignedChe();
				if (assignedChe != null) {
					assignedChe.removeWorkInstruction(wi); // necessary? new from v3
					changedChes.add(assignedChe);
				}
				OrderDetail owningDetail = wi.getParent();
				owningDetail.removeWorkInstruction(wi); // necessary? new from v3

				WorkInstruction.DAO.delete(wi);
			} catch (DaoException e) {
				LOGGER.error("failed to delete prior work instruction for CHE", e);
			}
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
					if (thisUse.getCurrentChe() != null) {
						changedChes.add(thisUse.getCurrentChe());
					}
					thisUse.setCurrentChe(inChe);
					try {
						ContainerUse.DAO.store(thisUse);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}

			}
		}

		for (Che changedChe : changedChes) {
			changedChe.getDao().pushNonPersistentUpdates(changedChe);
		}

		Timestamp theTime = new Timestamp(System.currentTimeMillis());

		// Get all of the OUTBOUND work instructions.
		wiResultList.addAll(generateOutboundInstructions(inChe, containerList, theTime));

		// Get all of the CROSS work instructions.
		wiResultList.addAll(generateCrossWallInstructions(inChe, containerList, theTime));

		WorkInstructionSequencerABC sequencer = getSequencer();
		List<WorkInstruction> sortedWIResults = sequencer.sort(this, wiResultList);
		return sortedWIResults.size();
	}

	private WorkInstructionSequencerABC getSequencer() {
		return WorkInstructionSequencerFactory.createSequencer(this.sequencerType);
	}

	private class GroupAndSortCodeComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			// watch for uninitialized data
			String sort1 = inWi1.getGroupAndSortCode();
			String sort2 = inWi2.getGroupAndSortCode();
			if (sort1 == null) {
				if (sort2 == null)
					return 0;
				else
					return -1;
			}
			if (sort2 == null)
				return 1;
			else
				return sort1.compareTo(sort2);
		}
	};

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

		// New from V4. make sure sorted correctly. Hard to believe we did not catch this before. (Should we have the DB sort for us?)
		Collections.sort(wiResultList, new GroupAndSortCodeComparator());
		return wiResultList;
	}

	private void deleteExistingShortWiToFacility(final OrderDetail inOrderDetail) {
		// Do we have short work instruction already for this orderDetail, for any CHE, going to facility?
		// Note, that leaves the shorts around that a user shorted.  This only delete the shorts created immediately upon scan if there is no product.

		// separate list to delete from, because we get ConcurrentModificationException if we delete in the middle of inOrderDetail.getWorkInstructions()
		List<WorkInstruction> aList = new ArrayList<WorkInstruction>();
		for (WorkInstruction wi : inOrderDetail.getWorkInstructions()) {
			if (wi.getStatusEnum() == WorkInstructionStatusEnum.SHORT)
				if (wi.getLocation().equals(this)) { // planned to the facility
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
				WorkInstruction.DAO.delete(wi);

			} catch (DaoException e) {
				LOGGER.error("failed to delete prior work SHORT instruction", e);
			}

		}

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

		if (itemMaster.getItemsOfUom(inOrderDetail.getUomMasterId()).size() == 0) {
			// If there is no item in inventory for that uom (AT ALL) then create a PLANNED, SHORT WI for this order detail.

			// Need to improve? Do we already have a short WI for this order detail? If so, do we really want to make another?
			// This should be moderately rare, although it happens in our test case over and over. User has to scan order/container to cart to make this happen.
			deleteExistingShortWiToFacility(inOrderDetail);
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
				boolean foundOne = false;
				// Item item = itemMaster.getFirstItemOnPath(path); // was this before v3
				String uomStr = inOrderDetail.getUomMasterId();
				Item item = itemMaster.getFirstItemMatchingUomOnPath(path, uomStr);

				if (item != null) {
					resultWi = createWorkInstruction(WorkInstructionStatusEnum.NEW,
						WorkInstructionTypeEnum.PLAN,
						inOrderDetail,
						inContainer,
						inChe,
						item.getStoredLocation(),
						inTime);
					if (resultWi != null)
						foundOne = true;
				}
				// We only want one work instruction made, not one per path.
				if (foundOne)
					break;
				// Bug remains, sort of. If cases exist on several paths, we would like to choose more intelligently which area to pick from.
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
								|| (outOrder.getOrderGroup() != null)
								&& (outOrder.getOrderGroup().equals(crossOrder.getOrderGroup()))) {
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
															(LocationABC) (firstOutOrderLoc.getLocation()),
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
		LocationABC inLocation,
		final Timestamp inTime) {
		WorkInstruction resultWi = null;
		boolean isInventoryPickInstruction = false;

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
				resultWi.setLedCmdStream("[]"); // empty array
			}

			// Set the LED lighting pattern for this WI.
			if (inStatus == WorkInstructionStatusEnum.SHORT) {
				// But not if it is a short WI (made to the facility location)
			} else if (inOrderDetail.getParent().getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) {
				// We currently have no use case that gets here. We never make direct work instruction from Cross order (which is a vendor put away).
				setCrossWorkInstructionLedPattern(resultWi,
					inOrderDetail.getItemMasterId(),
					inLocation,
					inOrderDetail.getUomMasterId());
			} else {
				// This might be a cross batch case! The work instruction came from cross batch order, but position and leds comes from the outbound order.
				// We could (should?) add a parameter to createWorkInstruction. Called from makeWIForOutbound() for normal outbound pick, and generateCrossWallInstructions().
				OrderHeader passedInDetailParent = inOrderDetail.getParent();

				// This test might be fragile. If it was a cross batch situation, then the orderHeader will have one or more locations.
				// If no order locations, then it must be a pick order. We want the leds for the inventory item.
				if (passedInDetailParent.getOrderLocations().size() == 0) {
					isInventoryPickInstruction = true;
					setOutboundWorkInstructionLedPatternAndPosAlongPathFromInventoryItem(resultWi,
						inLocation,
						inOrderDetail.getItemMasterId(),
						inOrderDetail.getUomMasterId());
				} else {
					// The cross batch situation. We want the leds for the order location(s)
					setWorkInstructionLedPatternFromOrderLocations(resultWi, passedInDetailParent);
				}
			}

			// Update the WI
			resultWi.setDomainId(Long.toString(System.currentTimeMillis()));
			resultWi.setTypeEnum(inType);
			resultWi.setStatusEnum(inStatus);

			resultWi.setLocation(inLocation);
			resultWi.setLocationId(inLocation.getFullDomainId());
			resultWi.setItemMaster(inOrderDetail.getItemMaster());
			String cookedDesc = WorkInstruction.cookDescription(inOrderDetail.getItemMaster().getDescription());
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
			if (inLocation instanceof Facility)
				resultWi.setPosAlongPath(0.0);
			else {
				if (isInventoryPickInstruction) {
					// do nothing as it was set with the leds
				} else {
					resultWi.setPosAlongPath(inLocation.getPosAlongPath());
				}
			}

			resultWi.setContainer(inContainer);
			resultWi.setAssignedChe(inChe);
			resultWi.setPlanQuantity(qtyToPick);
			resultWi.setPlanMinQuantity(minQtyToPick);
			resultWi.setPlanMaxQuantity(maxQtyToPick);
			resultWi.setActualQuantity(0);
			resultWi.setAssigned(inTime);
			try {
				WorkInstruction.DAO.store(resultWi);
				inOrderDetail.addWorkInstruction(resultWi); // This line new from v3
				inChe.addWorkInstruction(resultWi); // This line new from v3
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
	private void setWorkInstructionLedPatternFromOrderLocations(final WorkInstruction inWi, final OrderHeader inOrder) {
		// This is used for GoodEggs cross batch processs. The order header passed in is the outbound order (which has order locations),
		// but inWi was generated from the cross batch order detail.

		// Warning: the ledCmdStream must be set to "[]" if we bail. If not, site controller will NPE. Hence the check at this late stage
		// This does not bail intentionally. Perhap should if led = 0.
		String existingCmdString = inWi.getLedCmdStream();
		if (existingCmdString == null || existingCmdString.isEmpty()) {
			inWi.setLedCmdStream("[]"); // empty array
			LOGGER.error("work instruction was not initialized");
		}

		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();
		for (OrderLocation orderLocation : inOrder.getActiveOrderLocations()) {
			short firstLedPosNum = orderLocation.getLocation().getFirstLedNumAlongPath();
			short lastLedPosNum = orderLocation.getLocation().getLastLedNumAlongPath();

			// Put the positions into increasing order.
			if (firstLedPosNum > lastLedPosNum) {
				Short temp = firstLedPosNum;
				firstLedPosNum = lastLedPosNum;
				lastLedPosNum = temp;
			}

			// The new way of sending LED data to the remote controller. Note getEffectiveXXX instead of getLedController
			// This will throw if aisles/tiers are not configured yet. Lets avoid by the null checks.
			ISubLocation theLocation = orderLocation.getLocation(); // this should never be null by database constraint
			LedController theController = null;
			Short theChannel = 0;
			if (theLocation == null) {
				LOGGER.error("null order location in setWorkInstructionLedPatternFromOrderLocations. How?");
			} else {
				theController = theLocation.getEffectiveLedController();
				theChannel = theLocation.getEffectiveLedChannel();
			}
			// If this location has no controller, let's bail on led pattern
			if (theController == null || theChannel == null || theChannel == 0)
				continue; // just don't add a new ledCmdGrop to the WI command list

			List<LedSample> ledSamples = new ArrayList<LedSample>();
			LedCmdGroup ledCmdGroup = new LedCmdGroup(orderLocation.getLocation().getEffectiveLedController().getDeviceGuidStr(),
				orderLocation.getLocation().getEffectiveLedChannel(),
				firstLedPosNum,
				ledSamples);

			for (short ledPos = firstLedPosNum; ledPos < lastLedPosNum; ledPos++) {
				LedSample ledSample = new LedSample(ledPos, ColorEnum.BLUE);
				ledSamples.add(ledSample);
			}
			ledCmdGroup.setLedSampleList(ledSamples);
			ledCmdGroupList.add(ledCmdGroup);
		}
		inWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inOrder
	 */
	private void setOutboundWorkInstructionLedPatternAndPosAlongPathFromInventoryItem(final WorkInstruction inWi,
		final LocationABC inLocation,
		final String inItemMasterId,
		final String inUomId) {

		// Warning: the ledCmdStream must be set to "[]" if we bail. If not, site controller will NPE. Hence the check at this late stage
		String existingCmdString = inWi.getLedCmdStream();
		if (existingCmdString == null || existingCmdString.isEmpty()) {
			inWi.setLedCmdStream("[]"); // empty array
			LOGGER.error("work instruction was not initialized");
		}

		// This work instruction should have been generated from a pick order, so there must be inventory for the pick at the location.
		if (inWi == null || inLocation == null) {
			LOGGER.error("unexpected null condition in setOutboundWorkInstructionLedPatternFromInventoryItem");
			return;
		}
		if (inLocation instanceof Facility) {
			LOGGER.error("inappropriate call to  setOutboundWorkInstructionLedPatternFromInventoryItem");
			return;
		}

		// We expect to find an inventory item at the location. Be sure to get item and set posAlongPath always, before bailing out on the led command.
		Item theItem = inLocation.getStoredItemFromMasterIdAndUom(inItemMasterId, inUomId);
		if (theItem == null) {
			LOGGER.error("did not find item in setOutboundWorkInstructionLedPatternFromInventoryItem");
			return;
		}

		// Set the pos along path
		Double posAlongPath = theItem.getPosAlongPath();
		inWi.setPosAlongPath(posAlongPath);

		// if the location does not have led numbers, we do not have tubes or lasers there. Do not proceed.
		if (inLocation.getFirstLedNumAlongPath() == 0)
			return;

		// if the location does not have controller associated, we would NPE below. Might as well check now.
		LedController theLedController = inLocation.getEffectiveLedController();
		if (theLedController == null) {
			LOGGER.warn("Cannot set LED pattern on new pick WorkInstruction because no aisle controller for the item location ");
			return;
		}

		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();

		// Use our utility function to get the leds for the item
		LedRange theRange = theItem.getFirstLastLedsForItem();
		short firstLedPosNum = theRange.getFirstLedToLight();
		short lastLedPosNum = theRange.getLastLedToLight();
		// if the led number is zero, we do not have tubes or lasers there. Do not proceed.
		if (firstLedPosNum == 0)
			return;

		// This is how we send LED data to the remote controller. In this case, only one led sample range.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		LedCmdGroup ledCmdGroup = new LedCmdGroup(theLedController.getDeviceGuidStr(),
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

	// --------------------------------------------------------------------------
	/**
	 * Create the LED lighting pattern for the WI.
	 * @param inWi
	 * @param inOrderType
	 * @param inItemId
	 * @param inLocation
	 */
	private void setCrossWorkInstructionLedPattern(final WorkInstruction inWi,
		final String inItemMasterId,
		final LocationABC inLocation,
		final String inUom) {

		// Warning: the ledCmdStream must be set to "[]" if we bail. If not, site controller will NPE. Hence the check at this late stage
		// This does not bail intentionally. Perhap should if led = 0.
		String existingCmdString = inWi.getLedCmdStream();
		if (existingCmdString == null || existingCmdString.isEmpty()) {
			inWi.setLedCmdStream("[]"); // empty array
			LOGGER.error("work instruction was not initialized");
		}

		String itemDomainId = Item.makeDomainId(inItemMasterId, inLocation, inUom);
		short firstLedPosNum = inLocation.getFirstLedPosForItemId(itemDomainId);
		short lastLedPosNum = inLocation.getLastLedPosForItemId(itemDomainId);

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
			// That did the work. Big side effect. Deleted existing WIs for the CHE. Made new ones. Assigned container uses to the CHE.

			if (wiCount > 0) {
				// debug aid. Does the CHE know its work instructions?
				List<WorkInstruction> cheWiList = inChe.getCheWorkInstructions();
				Integer cheCountGot = cheWiList.size();
				if (cheCountGot != wiCount) {
					LOGGER.warn("setUpCheContainerFromString did not result in CHE getting all work instructions. Why?"); // Should this be an error? Maybe shorts do not go the CHE
				}

				// Get the work instructions for this CHE at this location for the given containers. Can we pass empty string? Normally user would scan where the CHE is starting.
				List<WorkInstruction> wiListAfterScanBlank = this.getWorkInstructions(inChe, ""); // cannot really scan blank, but this is how our UI simulation works
				Integer wiCountGot = wiListAfterScanBlank.size();
				// getWorkInstructions() has no side effects. But the site controller request gets these.
				// As work instructions are executed, they come back with start and complete time. and PLAN/NEW changes to ACTUAL/COMPLETE or ACTUAL/SHORT
				if (wiCountGot > 0) {
					// debug aid. Does the CHE know its work instructions?
					List<WorkInstruction> cheWiList2 = inChe.getCheWorkInstructions();
					Integer cheCountGot2 = cheWiList2.size();
					if (cheCountGot2 != wiCountGot) {
						LOGGER.warn("setUpCheContainerFromString did not result in CHE getting all work instructions. Why?"); // Should this be an error? Maybe shorts do not go the CHE
					}

				}

			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param outHeaderCounts
	 */
	public final HeaderCounts countCrossOrders() {
		return countOrders(OrderTypeEnum.CROSS);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param outHeaderCounts
	 */
	public final HeaderCounts countOutboundOrders() {

		return countOrders(OrderTypeEnum.OUTBOUND);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param outHeaderCounts
	 */
	private HeaderCounts countOrders(OrderTypeEnum inOrderTypeEnum) {
		int totalCrossHeaders = 0;
		int activeHeaders = 0;
		int activeDetails = 0;
		int activeCntrUses = 0;
		int inactiveDetailsOnActiveOrders = 0;
		int inactiveCntrUsesOnActiveOrders = 0;

		for (OrderHeader order : getOrderHeaders()) {
			if (order.getOrderTypeEnum().equals(inOrderTypeEnum)) {
				totalCrossHeaders++;
				if (order.getActive()) {
					activeHeaders++;

					ContainerUse cntrUse = order.getContainerUse();
					if (cntrUse != null)
						if (cntrUse.getActive())
							activeCntrUses++;
						else
							inactiveCntrUsesOnActiveOrders++;

					for (OrderDetail orderDetail : order.getOrderDetails()) {
						if (orderDetail.getActive())
							activeDetails++;
						else
							inactiveCntrUsesOnActiveOrders++;
						// if we were doing outbound orders, we might count WI here
					}
				}
			}
		}
		HeaderCounts outHeaderCounts = new HeaderCounts();
		outHeaderCounts.mTotalHeaders = totalCrossHeaders;
		outHeaderCounts.mActiveHeaders = activeHeaders;
		outHeaderCounts.mActiveDetails = activeDetails;
		outHeaderCounts.mActiveCntrUses = activeCntrUses;
		outHeaderCounts.mInactiveDetailsOnActiveOrders = inactiveDetailsOnActiveOrders;
		outHeaderCounts.mInactiveCntrUsesOnActiveOrders = inactiveCntrUsesOnActiveOrders;
		return outHeaderCounts;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWorkInstruction
	 */
	public final void sendWorkInstructionsToHost(final List<WorkInstruction> inWiList) {

		IronMqService ironMqService = getIronMqService(); // this should succeed, or catch its own throw and return null

		if (ironMqService != null) {
			ironMqService.sendWorkInstructionsToHost(inWiList);
		}
	}

	public Item upsertItem(String itemId, String storedLocationId, String cmDistanceFromLeft, String quantity, String inUomId) {
		//TODO This is a proof of concept and needs refactor to not have a dependency out of the EDI package
		storedLocationId = Strings.nullToEmpty(storedLocationId);

		InventoryCsvImporter importer = new InventoryCsvImporter(ItemMaster.DAO, Item.DAO, UomMaster.DAO);
		UomMaster uomMaster = importer.upsertUomMaster(inUomId, this);

		ItemMaster itemMaster = this.getItemMaster(itemId);
		InventorySlottedCsvBean itemBean = new InventorySlottedCsvBean();
		itemBean.setItemId(itemId);
		itemBean.setLocationId(storedLocationId);
		itemBean.setCmFromLeft(cmDistanceFromLeft);
		itemBean.setQuantity(quantity);
		itemBean.setUom(inUomId);
		LocationABC location = (LocationABC) this.findSubLocationById(storedLocationId);
		if (location == null && !Strings.isNullOrEmpty(storedLocationId)) {
			Errors errors = new DefaultErrors(Item.class);
			errors.rejectValue("storedLocation", ErrorCode.FIELD_NOT_FOUND, "storedLocation was not found");
			throw new InputValidationException(errors);
		}
		return importer.updateSlottedItem(false,
			itemBean,
			location,
			new Timestamp(System.currentTimeMillis()),
			itemMaster,
			uomMaster);
	}
}
