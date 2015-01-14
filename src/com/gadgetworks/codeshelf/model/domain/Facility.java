/*******************************************************************************

 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.proxy.HibernateProxy;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.InventorySlottedCsvBean;
import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.HeaderCounts;
import com.gadgetworks.codeshelf.model.HousekeepingInjector;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.WiFactory;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerABC;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerFactory;
import com.gadgetworks.codeshelf.model.WorkInstructionSequencerType;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.PropertyService;
import com.gadgetworks.codeshelf.util.CompareNullChecker;
import com.gadgetworks.codeshelf.util.UomNormalizer;
import com.gadgetworks.codeshelf.validation.BatchResult;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Facility
 *
 * The basic unit that holds all of the locations and equipment for a single facility.
 * At root of domain object tree, parent always null.
 *
 * @author jeffw
 */

@Entity
@DiscriminatorValue("FACILITY")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Facility extends Location {

	private static final String			IRONMQ_DOMAINID	= "IRONMQ";

	@Inject
	public static ITypedDao<Facility>	DAO;

	@Singleton
	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		@Inject
		public FacilityDao(PersistenceService persistenceService) {
			super(persistenceService);
		}

		@Override
		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}

		@Override
		public Facility findByDomainId(final IDomainObject parentObject, final String domainId) {
			return super.findByDomainId(null, domainId);
		}
	}

	private static final Logger				LOGGER				= LoggerFactory.getLogger(Facility.class);

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, Container>			containers			= new HashMap<String, Container>();

	@OneToMany(mappedBy = "parent")
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

	@OneToMany(mappedBy = "parent")
	@Getter
	private List<WorkInstruction>			workInstructions	= new ArrayList<WorkInstruction>();

	@Transient
	// for now installation specific.  property needs to be exposed as a configuration parameter.
	@Getter
	@Setter
	static WorkInstructionSequencerType		sequencerType		= WorkInstructionSequencerType.BayDistance;

	// TODO: replace with configuration via database table
	static {
		String sequencerConfig = System.getProperty("facility.sequencer");
		if ("BayDistance".equalsIgnoreCase(sequencerConfig)) {
			sequencerType = WorkInstructionSequencerType.BayDistance;
		} else if ("BayDistanceTopLast".equalsIgnoreCase(sequencerConfig)) {
			sequencerType = WorkInstructionSequencerType.BayDistanceTopLast;
		}
	}

	public Facility() {
		super();
	}

	@Override
	public Facility getFacility() {
		return this;
	}

	@Override
	public boolean isFacility() {
		return true;
	}

	public final static void setDao(ITypedDao<Facility> dao) {
		Facility.DAO = dao;
	}

	@Override
	public final String getDefaultDomainIdPrefix() {
		return "F";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<Facility> getDao() {
		return DAO;
	}

	@Override
	public final String getFullDomainId() {
		return getDomainId();
	}

	public final void setFacilityId(String inFacilityId) {
		setDomainId(inFacilityId);
	}

	public final void addAisle(Aisle inAisle) {
		Location previousFacility = inAisle.getParent();
		if (previousFacility == null) {
			this.addLocation(inAisle);
			inAisle.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add Aisle " + inAisle.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final void removeAisle(Aisle inAisle) {
		String domainId = inAisle.getDomainId();
		if (this.getLocations().get(domainId) != null) {
			inAisle.setParent(null);
			this.removeLocation(domainId);
		} else {
			LOGGER.error("cannot remove Aisle " + inAisle.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void addPath(Path inPath) {
		Facility facility = inPath.getParent();
		if (facility != null && !facility.equals(this)) {
			LOGGER.error("cannot add Path " + inPath.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + facility.getDomainId());
			return;
		}
		paths.put(inPath.getDomainId(), inPath);
		inPath.setParent(this);
	}

	public final Path getPath(String inPathId) {
		return paths.get(inPathId);
	}

	public final List<Path> getPaths() {
		ArrayList<Path> arrayPaths = new ArrayList<Path>();
		for(Path p : paths.values()) {
			if (p instanceof HibernateProxy) {
				arrayPaths.add(PersistenceService.<Path>deproxify(p));
			} else {
				arrayPaths.add(p);
			}
		}
		return arrayPaths;
	}

	public final void removePath(String inPathId) {
		Path path = this.getPath(inPathId);
		if (path != null) {
			path.setParent(null);
			paths.remove(inPathId);
		} else {
			LOGGER.error("cannot remove Path " + inPathId + " from " + this.getDomainId() + " because it isn't found in children");
		}
	}

	public final void addContainer(Container inContainer) {
		Facility previousFacility = inContainer.getParent();
		if (previousFacility == null) {
			containers.put(inContainer.getDomainId(), inContainer);
			inContainer.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add Container " + inContainer.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final Container getContainer(String inContainerId) {
		return containers.get(inContainerId);
	}

	public final List<Container> getContainers() {
		return new ArrayList<Container>(containers.values());
	}

	public final void removeContainer(String inContainerId) {
		Container container = this.getContainer(inContainerId);
		if (container != null) {
			container.setParent(null);
			containers.remove(inContainerId);
		} else {
			LOGGER.error("cannot remove Container " + inContainerId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void addContainerKind(ContainerKind inContainerKind) {
		Facility previousFacility = inContainerKind.getParent();
		if (previousFacility == null) {
			containerKinds.put(inContainerKind.getDomainId(), inContainerKind);
			inContainerKind.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add ContainerKind " + inContainerKind.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final ContainerKind getContainerKind(String inContainerKindId) {
		return containerKinds.get(inContainerKindId);
	}

	public final void removeContainerKind(String inContainerKindId) {
		ContainerKind containerKind = this.getContainerKind(inContainerKindId);
		if (containerKind != null) {
			containerKind.setParent(null);
			containerKinds.remove(inContainerKindId);
		} else {
			LOGGER.error("cannot remove ContainerKind " + inContainerKindId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void addEdiService(IEdiService inEdiService) {
		Facility previousFacility = inEdiService.getParent();
		if (previousFacility == null) {
			ediServices.add(inEdiService);
			inEdiService.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add EdiService " + inEdiService.getServiceName() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final void removeEdiService(IEdiService inEdiService) {
		if (this.ediServices.contains(inEdiService)) {
			inEdiService.setParent(null);
			ediServices.remove(inEdiService);
		} else {
			LOGGER.error("cannot remove EdiService " + inEdiService.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void addWorkInstruction(WorkInstruction wi) {
		Facility previousFacility = wi.getParent();
		if (previousFacility == null) {
			int numWi = workInstructions.size();
			boolean trans = PersistenceService.getInstance().hasActiveTransaction();
			workInstructions.add(wi);
			wi.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add WorkInstruction " + wi.getPersistentId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId(), new Exception());
		}
	}

	public final void removeWorkInstruction(WorkInstruction wi) {
		if (this.workInstructions.contains(wi)) {
			wi.setParent(null);
			workInstructions.remove(wi);
		} else {
			LOGGER.error("cannot remove WorkInstruction " + wi.getPersistentId() + " from " + this.getDomainId()
					+ " because it isn't found in children", new Exception());
		}
	}

	public final void addItemMaster(ItemMaster inItemMaster) {
		Facility previousFacility = inItemMaster.getParent();
		if (previousFacility == null) {
			itemMasters.put(inItemMaster.getDomainId(), inItemMaster);
			inItemMaster.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add ItemMaster " + inItemMaster.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId(), new Exception());
		}
	}

	public final ItemMaster getItemMaster(String inItemMasterId) {
		return itemMasters.get(inItemMasterId);
	}

	public final void removeItemMaster(String inItemMasterId) {
		ItemMaster itemMaster = this.getItemMaster(inItemMasterId);
		if (itemMaster != null) {
			itemMaster.setParent(null);
			itemMasters.remove(inItemMasterId);
		} else {
			LOGGER.error("cannot remove ItemMaster " + inItemMasterId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final List<ItemMaster> getItemMasters() {
		return new ArrayList<ItemMaster>(itemMasters.values());
	}

	public final void addOrderGroup(OrderGroup inOrderGroup) {
		Facility previousFacility = inOrderGroup.getParent();
		if (previousFacility == null) {
			orderGroups.put(inOrderGroup.getDomainId(), inOrderGroup);
			inOrderGroup.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add OrderGroup " + inOrderGroup.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final OrderGroup getOrderGroup(String inOrderGroupId) {
		return orderGroups.get(inOrderGroupId);
	}

	public final void removeOrderGroup(String inOrderGroupId) {
		OrderGroup orderGroup = this.getOrderGroup(inOrderGroupId);
		if (orderGroup != null) {
			orderGroup.setParent(null);
			orderGroups.remove(inOrderGroupId);
		} else {
			LOGGER.error("cannot remove OrderGroup " + inOrderGroupId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final List<OrderGroup> getOrderGroups() {
		return new ArrayList<OrderGroup>(orderGroups.values());
	}

	public final void addOrderHeader(OrderHeader inOrderHeader) {
		Facility previousFacility = inOrderHeader.getParent();
		if (previousFacility != null && !previousFacility.equals(this)) {
			LOGGER.error("cannot add OrderHeader " + inOrderHeader.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		} else {
			orderHeaders.put(inOrderHeader.getDomainId(), inOrderHeader);
			inOrderHeader.setParent(this);
		}
	}

	public final OrderHeader getOrderHeader(String inOrderHeaderId) {
		return orderHeaders.get(inOrderHeaderId);
	}

	public final void removeOrderHeader(String inOrderHeaderId) {
		OrderHeader orderHeader = this.getOrderHeader(inOrderHeaderId);
		if (orderHeader != null) {
			orderHeader.setParent(null);
			orderHeaders.remove(inOrderHeaderId);
		} else {
			LOGGER.error("cannot remove OrderHeader " + inOrderHeaderId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final List<OrderHeader> getOrderHeaders() {
		return new ArrayList<OrderHeader>(orderHeaders.values());
	}

	public final void addUomMaster(UomMaster inUomMaster) {
		Facility previousFacility = inUomMaster.getParent();
		if (previousFacility == null) {
			uomMasters.put(inUomMaster.getDomainId(), inUomMaster);
			inUomMaster.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add UomMaster " + inUomMaster.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final UomMaster getUomMaster(String inUomMasterId) {
		return uomMasters.get(inUomMasterId);
	}

	public final void removeUomMaster(String inUomMasterId) {
		UomMaster uomMaster = this.getUomMaster(inUomMasterId);
		if (uomMaster != null) {
			uomMaster.setParent(null);
			uomMasters.remove(inUomMasterId);
		} else {
			LOGGER.error("cannot remove UomMaster " + inUomMasterId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void addNetwork(CodeshelfNetwork inNetwork) {
		Facility previousFacility = inNetwork.getParent();
		if (previousFacility == null) {
			networks.put(inNetwork.getDomainId(), inNetwork);
			inNetwork.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add CodeshelfNetwork " + inNetwork.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final CodeshelfNetwork getNetwork(String inNetworkId) {
		return networks.get(inNetworkId);
	}

	public final void removeNetwork(String inNetworkId) {
		CodeshelfNetwork network = this.getNetwork(inNetworkId);
		if (network != null) {
			network.setParent(null);
			networks.remove(inNetworkId);
		} else {
			LOGGER.error("cannot remove CodeshelfNetwork " + inNetworkId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final List<CodeshelfNetwork> getNetworks() {
		return new ArrayList<CodeshelfNetwork>(networks.values());
	}

	public final void addLocationAlias(LocationAlias inLocationAlias) {
		Facility previousFacility = inLocationAlias.getParent();
		if (previousFacility == null) {

			locationAliases.put(inLocationAlias.getDomainId(), inLocationAlias);
			inLocationAlias.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add LocationAlias " + inLocationAlias.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public final LocationAlias getLocationAlias(String inLocationAliasId) {
		return locationAliases.get(inLocationAliasId);
	}

	public final void removeLocationAlias(LocationAlias inLocationAlias) {
		String locationAliasId = inLocationAlias.getDomainId();
		LocationAlias locationAlias = this.getLocationAlias(locationAliasId);
		if (locationAlias != null) {
			locationAlias.setParent(null);
			locationAliases.remove(locationAliasId);
		} else {
			LOGGER.error("cannot remove LocationAlias " + locationAliasId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final List<LocationAlias> getLocationAliases() {
		return new ArrayList<LocationAlias>(locationAliases.values());
	}

	@Override
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

	@JsonProperty("primaryChannel")
	public Short getPrimaryChannel() {
		CodeshelfNetwork network = this.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		return network.getChannel();
	}

	@JsonProperty("primaryChannel")
	public void setPrimaryChannel(Short channel) {
		CodeshelfNetwork network = this.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		network.setChannel(channel);
		network.getDao().store(network);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inProtoBayWidthMeters
	 * @param inBaysLong
	 * @param inRunInXDir
	 * @return
	 */
	@SuppressWarnings("unused")
	private Point computePickFaceEndPoint(final Point inAnchorPoint, final Double inDistanceMeters, final Boolean inRunInXDir) {
		Point result;
		if (inRunInXDir) {
			result = new Point(PositionTypeEnum.METERS_FROM_PARENT, inAnchorPoint.getX() + inDistanceMeters, 0.0, 0.0);
		} else {
			result = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, inAnchorPoint.getY() + inDistanceMeters, 0.0);
		}
		return result;
	}

	private String nextPathDomainId() {
		// The first path is domainId.1, etc.
		// Currently, path are deleted, and not merely inactivated. So no need to worry about reactivating paths
		String facilityID = getDomainId();
		String pathId;
		Integer index = 0;
		while (true) {
			index++;
			pathId = facilityID + "." + index;
			Path existingPath = Path.DAO.findByDomainId(this, pathId);
			if (existingPath == null)
				break;
			// just fear of infinite loops
			if (index > 100) {
				LOGGER.error("infinite loop in nextPathDomainId()");
				break;
				// should throw later on database constraint disallowing duplicate
			}
		}
		return pathId;
	}

	public final Path createPath(String inDomainId) {
		Path path = new Path();
		// Start keeping the API, but not respecting the suggested domainId
		String pathDomainId = nextPathDomainId();
		if (!inDomainId.isEmpty() && !pathDomainId.equals(inDomainId)) {
			LOGGER.warn("revise createPath() caller or API");
		}
		path.setDomainId(pathDomainId);

		this.addPath(path);
		Path.DAO.store(path);

		path.createDefaultWorkArea(); //TODO an odd way to construct, but it is a way to make sure the Path is persisted before the work area
		return path;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a path
	 *
	 */
	public final Path createPath(String inDomainId, PathSegment[] inPathSegments) {
		Path path = createPath(inDomainId);
		// Started above by keeping the API, but not respecting the suggested domainId. See what domainId we actually got
		String pathDomainId = path.getDomainId();
		String segmentDomainId;
		Integer index = -1;

		for (PathSegment pathSegment : inPathSegments) {
			index++;
			// Just checking that the UI or whatever called this set the correct order
			if (!index.equals(pathSegment.getSegmentOrder()))
				LOGGER.error("looks like incorrect segment orders in createPath()");
			segmentDomainId = pathDomainId + "." + pathSegment.getSegmentOrder();
			pathSegment.setDomainId(segmentDomainId);

			path.addPathSegment(pathSegment);
			PathSegment.DAO.store(pathSegment);
		}
		return path;
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
			for (LocationABC location : segment.getLocations()) {
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
				for (Location location : segment.getLocations()) {
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
	public final void createOrUpdateVertices(Location inLocation, Point inDimMeters) {
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
					Vertex vertexN = new Vertex();
					vertexN.setDomainId(vertexNames[n]);
					vertexN.setDrawOrder(n);
					vertexN.setPoint(points[n]);

					// Interesting bug. Drop aisle works. Redrop while still running lead to error if addVertex not there. Subsequent redrops after application start ok.
					inLocation.addVertex(vertexN);
					Vertex.DAO.store(vertexN);
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
	public final void createDefaultContainerKind() {
		//ContainerKind containerKind = 
		createContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND, 0.0, 0.0, 0.0);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final ContainerKind createContainerKind(String inDomainId,
		Double inLengthMeters,
		Double inWidthMeters,
		Double inHeightMeters) {

		ContainerKind result = null;

		result = new ContainerKind();
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
	public final IEdiService getEdiExportService() {
		return IronMqService.DAO.findByDomainId(this, IRONMQ_DOMAINID);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final IronMqService createIronMqService() throws PSQLException {
		// we saw the PSQL exception in staging test when the record could not be added
		IronMqService result = null;

		result = new IronMqService();
		result.setDomainId(IRONMQ_DOMAINID);
		result.setProvider(EdiProviderEnum.IRONMQ);
		result.setServiceState(EdiServiceStateEnum.UNLINKED);
		this.addEdiService(result);
		result.storeCredentials("", ""); // non-null credentials
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
				break;
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final DropboxService createDropboxService() {

		DropboxService result = null;

		result = new DropboxService();
		result.setDomainId("DROPBOX");
		result.setProvider(EdiProviderEnum.DROPBOX);
		result.setServiceState(EdiServiceStateEnum.UNLINKED);

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
	public final CodeshelfNetwork createNetwork(final String inNetworkName) {

		CodeshelfNetwork result = null;

		result = new CodeshelfNetwork();
		result.setDomainId(inNetworkName);
		result.setActive(true);
		result.setDescription("");
		//result.setCredential(Double.toString(Math.random()));
		//result.setCredential("0.6910096026612129");

		this.addNetwork(result);

		try {
			CodeshelfNetwork.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("DaoException persistence error storing CodeshelfNetwork", e);
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
	public final List<WorkInstruction> computeWorkInstructions(final Che inChe, final List<String> inContainerIdList) {

		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();

		//manually track changed ches here to trigger an update broadcast
		Set<Che> changedChes = new HashSet<Che>();
		changedChes.add(inChe);

		// This is ugly. We probably do want a housekeeping type here, but then might want subtypes not in this query
		Collection<WorkInstructionTypeEnum> wiTypes = new ArrayList<WorkInstructionTypeEnum>(3);
		wiTypes.add(WorkInstructionTypeEnum.PLAN);
		wiTypes.add(WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		wiTypes.add(WorkInstructionTypeEnum.HK_REPEATPOS);

		// Delete any planned WIs for this CHE.
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("assignedChe.persistentId", inChe.getPersistentId()));
		filterParams.add(Restrictions.in("type", wiTypes));
		List<WorkInstruction> wis = WorkInstruction.DAO.findByFilter(filterParams);
		for (WorkInstruction wi : wis) {
			try {

				Che assignedChe = wi.getAssignedChe();
				if (assignedChe != null) {
					assignedChe.removeWorkInstruction(wi); // necessary? new from v3
					changedChes.add(assignedChe);
				}
				OrderDetail owningDetail = wi.getOrderDetail();
				// detail is optional from v5
				if (owningDetail != null)
					owningDetail.removeWorkInstruction(wi); // necessary? new from v3

				WorkInstruction.DAO.delete(wi);
			} catch (DaoException e) {
				LOGGER.error("failed to delete prior work instruction for CHE", e);
			}
		}

		// DEV-492 identify previous container uses
		ArrayList<ContainerUse> priorCntrUses = new ArrayList<ContainerUse>();
		priorCntrUses.addAll(inChe.getUses());
		ArrayList<ContainerUse> newCntrUses = new ArrayList<ContainerUse>();

		// Set new uses on the CHE.
		List<Container> containerList = new ArrayList<Container>();
		for (String containerId : inContainerIdList) {
			Container container = getContainer(containerId);
			if (container != null) {
				// add to the list that will generate work instructions
				containerList.add(container);
				// Set the CHE on the containerUse
				ContainerUse thisUse = container.getCurrentContainerUse();
				if (thisUse != null) {
					newCntrUses.add(thisUse); // DEV-492 bit
					Che previousChe = thisUse.getCurrentChe();
					if (previousChe != null) {
						changedChes.add(previousChe);
					}
					if (previousChe == null) {
						inChe.addContainerUse(thisUse);
					} else if (!previousChe.equals(inChe)) {
						previousChe.removeContainerUse(thisUse);
						inChe.addContainerUse(thisUse);
					}
				}

				try {
					ContainerUse.DAO.store(thisUse);
				} catch (DaoException e) {
					LOGGER.error("", e);
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
					ContainerUse.DAO.store(oldUse);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}

		/*
		Che.DAO.clearAllCaches();

		for (Che changedChe : changedChes) {
			changedChe.getDao().pushNonPersistentUpdates(changedChe);
		}
		 */

		Timestamp theTime = new Timestamp(System.currentTimeMillis());

		// Get all of the OUTBOUND work instructions.
		wiResultList.addAll(generateOutboundInstructions(inChe, containerList, theTime));

		// Get all of the CROSS work instructions.
		wiResultList.addAll(generateCrossWallInstructions(inChe, containerList, theTime));

		//Filter,Sort, and save actionsable WI's
		//TODO Consider doing this in getWork?
		this.sortAndSaveActionableWIs(wiResultList);
		//It uses the iterater or remove items from the existing list and add it to the new one
		//If all we care about are the counts. Why do we even sort them now?

		LOGGER.info("TOTAL WIs {}", wiResultList);
		
		//Return original full list
		return wiResultList;
	}

	private void sortAndSaveActionableWIs(List<WorkInstruction> allWIs) {
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
		List<WorkInstruction> sortedWIResults = getSequencer().sort(this, allWIs);

		//Save sort
		WorkInstructionSequencerABC.setSortCodesByCurrentSequence(sortedWIResults);
	}

	private WorkInstructionSequencerABC getSequencer() {
		return WorkInstructionSequencerFactory.createSequencer(Facility.getSequencerType());
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
	private Double getStartingPathDistance(final String inScannedLocationId) {
		if (inScannedLocationId == null || inScannedLocationId.isEmpty())
			return 0.0;

		Location cheLocation = null;
		cheLocation = findSubLocationById(inScannedLocationId);
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
						&& (bay.getPosAlongPath() + Location.BAY_ALIGNMENT_FUDGE > cheBay.getPosAlongPath())) {
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
	private void queryAddCheInstructionsToList(final Che inChe, final Double inFromStartingPosition, List<WorkInstruction> inWiList) {
		if (inChe == null || inFromStartingPosition == null || inWiList == null) {
			LOGGER.error("null input to queryAddCheInstructionsToList");
			return;
		}

		Collection<WorkInstructionTypeEnum> wiTypes = new ArrayList<WorkInstructionTypeEnum>(3);
		wiTypes.add(WorkInstructionTypeEnum.PLAN);
		wiTypes.add(WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		wiTypes.add(WorkInstructionTypeEnum.HK_REPEATPOS);

		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("assignedChe", inChe));
		filterParams.add(Restrictions.in("type", wiTypes));
		filterParams.add(Restrictions.ge("posAlongPath", inFromStartingPosition));

		//String filter = "(assignedChe.persistentId = :chePersistentId) and (typeEnum = :type) and (posAlongPath >= :pos)";
		//throw new NotImplementedException("Needs to be implemented with a custom query");

		// Hibernate version has test failing with database lock here, so pull out the query
		List<WorkInstruction> filterWiList = WorkInstruction.DAO.findByFilter(filterParams);

		for (WorkInstruction wi : filterWiList) {
			// Very unlikely. But if some wLocationABCs were deleted between start work and scan starting location, let's not give out the "deleted" wis
			// Note: puts may have had multiple order locations, now quite denormalized on WI fields and hard to decompose.  We just take the first as the WI location.
			// Not ambiguous for picks.
			Location loc = wi.getLocation();
			// so far, wi must have a location. Even housekeeping and shorts
			if (loc == null)
				LOGGER.error("getWorkInstructions found active work instruction with null location"); // new log message from v8. Don't expect any null.
			else if (loc.isActive()) //unlikely that location got deleted between complete work instructions and scan location
				inWiList.add(wi);
			else
				LOGGER.warn("getWorkInstructions found active work instruction in deleted locations"); // new from v8
		}
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
		long startTimestamp = System.currentTimeMillis();

		//Get current complete list of WIs
		List<WorkInstruction> completeRouteWiList = new ArrayList<WorkInstruction>();
		queryAddCheInstructionsToList(inChe, 0.0, completeRouteWiList);
		
		//We could have existing HK WIs if we've already retrieved the work instructions once but scanned a new location.
		//In that case, we must make sure we remove all existing HK WIs so that we can properly add them back in at the end.
		//We may want to consider not hitting the database for this. It is easiest/safest option for now.
		for (Iterator<WorkInstruction> wiIter = completeRouteWiList.iterator(); wiIter.hasNext();) {
			WorkInstruction wi = wiIter.next();
			if (wi.isHousekeeping()) {
				LOGGER.info("Removing exisiting HK WI={}", wi);
				WorkInstruction.DAO.delete(wi);
				wiIter.remove();
			}
		}

		Double startingPathPos = getStartingPathDistance(inScannedLocationId);
		if (startingPathPos == null) {
			// getStartingPathDistance logged the errors, so we do not need to. Just return the empty list.
			return Lists.newArrayList();
		}

		// Get all of the PLAN WIs assigned to this CHE beyond the specified position
		List<WorkInstruction> wiListFromStartLocation = new ArrayList<WorkInstruction>();
		queryAddCheInstructionsToList(inChe, startingPathPos, wiListFromStartLocation);

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
			wrappedRouteWiList = HousekeepingInjector.addHouseKeepingAndSaveSort(this, wrappedRouteWiList);
		}

		//Log time if over 2 seconds
		Long wrapComputeDurationMs = System.currentTimeMillis() - startTimestamp;
		if (wrapComputeDurationMs > 2000) {
			LOGGER.warn("GetWork() took {}; totalWis={};", wrapComputeDurationMs, wrappedRouteWiList.size());
		}

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
			resultWi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.SHORT,
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
				String uomStr = inOrderDetail.getUomMasterId();
				Item item = itemMaster.getFirstActiveItemMatchingUomOnPath(path, uomStr);

				if (item != null) {
					resultWi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW,
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
		int count = 0;

		// To proceed, there should container use linked to outbound order
		// We want to add all orders represented in the container list because these containers (or for Accu, fake containers representing the order) were scanned for this CHE to do.
		for (Container container : inContainerList) {
			OrderHeader order = container.getCurrentOrderHeader();
			if (order != null && order.getOrderType().equals(OrderTypeEnum.OUTBOUND)) {
				boolean somethingDone = false;
				for (OrderDetail orderDetail : order.getOrderDetails()) {
					// An order detail might be set to zero quantity by customer, essentially canceling that item. Don't make a WI if canceled.
					if (orderDetail.getQuantity() > 0) {
						count++;
						LOGGER.debug("WI #" + count + "in generateOutboundInstructions");
						WorkInstruction aWi = makeWIForOutbound(orderDetail, inChe, container, inTime); // Could be normal WI, or a short WI
						if (aWi != null) {
							wiResultList.add(aWi);
							somethingDone = true;

							// still do this for a short WI?
							orderDetail.setStatus(OrderStatusEnum.INPROGRESS);
							try {
								OrderDetail.DAO.store(orderDetail);
							} catch (DaoException e) {
								LOGGER.error("", e);
							}

						} else if (orderDetail.getStatus() == OrderStatusEnum.COMPLETE) {
							//As of DEV-561 we are adding completed WIs to the list in order to be able
							//give feedback on complete orders (and differentiate a 100% complete order from
							//unknown container id. The computeWork method will filter these out before sorting
							//and saving
							LOGGER.info("Adding already complete WIs to list; orderDetail={}", orderDetail);
							wiResultList.addAll(orderDetail.getWorkInstructions());
						}
					}
				}
				if (somethingDone) {
					order.setStatus(OrderStatusEnum.INPROGRESS);
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

		List<WorkInstruction> wiList = Lists.newArrayList();
		for (Container container : inContainerList) {
			BatchResult<Work> result = determineWorkForContainer(container);
			for (Work work : result.getResult()) {
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
			}
		}
		return wiList;
	}

	public BatchResult<Work> determineWorkForContainer(Container container) {
		// Iterate over all active CROSS orders on the path.
		BatchResult<Work> batchResult = new BatchResult<Work>();
		OrderHeader crossOrder = container.getCurrentOrderHeader();
		if ((crossOrder != null) && (crossOrder.getActive()) && (crossOrder.getOrderType().equals(OrderTypeEnum.CROSS))) {
			List<OrderDetail> matchingOrderDetails = toAllMatchingOutboundOrderDetails(crossOrder);
			for (OrderDetail matchingOutboundOrderDetail : matchingOrderDetails) {
				List<Location> firstOrderLocationPerPath = toPossibleLocations(matchingOutboundOrderDetail);
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

	/**
	 * toPossibleLocations will return a list, but the list may be empty
	 */
	private List<Location> toPossibleLocations(OrderDetail matchingOutboundOrderDetail) {
		ArrayList<Location> locations = new ArrayList<Location>();
		for (Path path : getPaths()) {
			OrderLocation firstOutOrderLoc = matchingOutboundOrderDetail.getParent().getFirstOrderLocationOnPath(path);
			if (firstOutOrderLoc != null)
				locations.add(firstOutOrderLoc.getLocation());
		}
		return locations;
	}

	private List<OrderDetail> toAllMatchingOutboundOrderDetails(OrderHeader crossbatchOrder) {
		List<OrderDetail> allMatchingOrderDetails = Lists.newArrayList();
		for (OrderDetail crossOrderDetail : crossbatchOrder.getOrderDetails()) {
			if (crossOrderDetail.getActive()) {
				List<OrderDetail> matchingOrderDetails = toMatchingOutboundOrderDetail(crossOrderDetail);
				allMatchingOrderDetails.addAll(matchingOrderDetails);
			}
		}
		return allMatchingOrderDetails;
	}

	private List<OrderDetail> toMatchingOutboundOrderDetail(OrderDetail crossbatchOrderDetail) {
		Preconditions.checkNotNull(crossbatchOrderDetail);
		Preconditions.checkArgument(crossbatchOrderDetail.getActive());
		Preconditions.checkArgument(crossbatchOrderDetail.getParent().getOrderType().equals(OrderTypeEnum.CROSS));

		List<OrderDetail> matchingOutboundOrderDetail = new ArrayList<OrderDetail>();
		for (OrderHeader outOrder : getOrderHeaders()) {
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

		inWi.setPickInstruction(locationString);

		try {
			WorkInstruction.DAO.store(inWi);
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
						WorkInstruction.DAO.store(wi);
						wiIterator.remove();
					}
				}
			}
		}
		return wiResultList;
	}

	/**
	 * Compare Items by their ItemMasterDdc.
	 *
	 */
	private class DdcItemComparator implements Comparator<Item> {

		@Override
		public int compare(Item inItem1, Item inItem2) {
			return inItem1.getParent().getDdcId().compareTo(inItem2.getParent().getDdcId());
		}
	};

	/**
	 * Compare ItemMasters by their DDC.
	 */
	private class DdcItemMasterComparator implements Comparator<ItemMaster> {

		@Override
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

		List<Location> ddcLocations = getDdcLocations();
		List<ItemMaster> ddcItemMasters = getDccItemMasters();

		// Sort the DDC items in lex/DDC order.
		LOGGER.debug("DDC sort items");
		Collections.sort(ddcItemMasters, new DdcItemMasterComparator());

		// Get the items that belong to each DDC location.
		LOGGER.debug("DDC list items");
		List<Item> locationItems = new ArrayList<Item>();
		Double locationItemsQuantity;
		for (Location location : ddcLocations) {
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
		Location inLocation,
		Double inLocationLen) {

		Double ddcPos = inLocation.getPosAlongPath();
		Double distPerItem = inLocationLen / inLocationItemsQuantity;
		ItemDdcGroup lastDdcGroup = null;
		Collections.sort(inLocationItems, new DdcItemComparator());
		for (Item item : inLocationItems) {
			ddcPos += distPerItem * item.getQuantity();
			item.setPosAlongPath(ddcPos);
			try {
				Item.DAO.store(item);
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

				lastDdcGroup.setStartPosAlongPath(item.getPosAlongPath());
				item.getStoredLocation().addItemDdcGroup(lastDdcGroup);
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
		Location inLocation) {
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
	private Double computeLengthOfLocationFace(Location inLocation) {
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
	 * Not currently used. An old stitch-fix feature.  Remember that getActiveChildren() is recursive, so it returns bays, tiers, or slots--whatever has the Ddc.
	 */
	private List<Location> getDdcLocations() {
		LOGGER.debug("DDC get locations");
		List<Location> ddcLocations = new ArrayList<Location>();
		for (Location aisle : getActiveChildrenAtLevel(Aisle.class)) {
			// no actual need for above line. facility.getActiveChildren would work equally
			for (Location location : aisle.getActiveChildren()) {
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

		CodeshelfNetwork network = getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		if (network == null)
			return result;
		Map<String, LedController> controllerMap = network.getLedControllers();
		result = controllerMap.size();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * The UI needs this answer. UI gets it at login. Does not live update to the UI.
	 */
	@JsonProperty("hasCrossBatchOrders")
	public final boolean hasCrossBatchOrders() {
		// DEV-582 ties this to the config parameter. Used to be inferred from the data
		String theValue = PropertyService.getPropertyFromConfig(this, DomainObjectProperty.CROSSBCH);
		boolean result = Boolean.parseBoolean(theValue);
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * The UI needs this answer. UI gets it at login.
	 * If true, the UI wants to believe that ALL crossbatch and outbound orders have an order group. The orders view will not show at all any orders without a group.
	 */
	@JsonProperty("hasMeaningfulOrderGroups")
	public final boolean hasMeaningfulOrderGroups() {
		// We really want to change to a config parameter, and then pass to the UI a three-value choice:
		// Only two-level Orders view, only three-level, or both 2 and 3-level.		

		List<OrderGroup> groupsList = this.getOrderGroups();
		boolean result = groupsList.size() > 0;
		// clearly might give the wrong value if site is initially misconfigured. Could look at the orderHeaders in more detail. Do most have groups?

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inChe
	 * @param inContainers
	 * Testing only!  passs in as 23,46,2341a23. This yields container ID 23 in slot1, container Id 46 in slot 2, etc.
	 *
	 */
	public final void setUpCheContainerFromString(Che inChe, String inContainers) {
		if (inChe == null)
			return;

		// computeWorkInstructions wants a containerId list
		List<String> containersIdList = Arrays.asList(inContainers.split("\\s*,\\s*")); // this trims out white space

		if (containersIdList.size() > 0) {
			this.computeWorkInstructions(inChe, containersIdList);
			// That did the work. Big side effect. Deleted existing WIs for the CHE. Made new ones. Assigned container uses to the CHE.
			// The wiCount returned is mainly or convenience and debugging. It may not include some shorts
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
			if (order.getOrderType().equals(inOrderTypeEnum)) {
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

	public Item upsertItem(String itemId, String storedLocationId, String cmDistanceFromLeft, String quantity, String inUomId) {
		//TODO This is a proof of concept and needs refactor to not have a dependency out of the EDI package
		storedLocationId = Strings.nullToEmpty(storedLocationId);

		InventoryCsvImporter importer = new InventoryCsvImporter(new EventProducer(), ItemMaster.DAO, Item.DAO, UomMaster.DAO);
		UomMaster uomMaster = importer.upsertUomMaster(inUomId, this);

		ItemMaster itemMaster = this.getItemMaster(itemId);
		InventorySlottedCsvBean itemBean = new InventorySlottedCsvBean();
		itemBean.setItemId(itemId);
		itemBean.setLocationId(storedLocationId);
		itemBean.setCmFromLeft(cmDistanceFromLeft);
		itemBean.setQuantity(quantity);
		itemBean.setUom(inUomId);
		Location location = this.findSubLocationById(storedLocationId);
		if (location == null && !Strings.isNullOrEmpty(storedLocationId)) {
			DefaultErrors errors = new DefaultErrors(Item.class);
			errors.rejectValue("storedLocation", storedLocationId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
			throw new InputValidationException(errors);
		}

		Item returnItem = importer.updateSlottedItem(false,
			itemBean,
			location,
			new Timestamp(System.currentTimeMillis()),
			itemMaster,
			uomMaster);
		return returnItem;
	}

	private final Set<SiteController> getSiteControllers() {
		Set<SiteController> siteControllers = new HashSet<SiteController>();

		for (CodeshelfNetwork network : this.getNetworks()) {
			siteControllers.addAll(network.getSiteControllers().values());
		}
		return siteControllers;
	}

	public final Set<User> getSiteControllerUsers() {
		Set<User> users = new HashSet<User>();

		for (SiteController sitecon : this.getSiteControllers()) {
			User user = sitecon.getAuthenticationUser();
			if (user != null) {
				users.add(user);
			} else {
				LOGGER.warn("Couldn't find user for site controller " + sitecon.getDomainId());
			}
		}
		return users;
	}

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

	public Aisle createAisle(String inAisleId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		Aisle aisle = new Aisle();
		aisle.setDomainId(inAisleId);
		aisle.setAnchorPoint(inAnchorPoint);
		aisle.setPickFaceEndPoint(inPickFaceEndPoint);

		this.addAisle(aisle);

		return aisle;
	}

	@Override
	public Location getParent() {
		return null;
	}

	@Override
	public void setParent(Location inParent) {
		if (inParent != null) {
			String msg = "tried to set Facility " + this.getDomainId() + " parent to non-null " + inParent.getClassName() + " "
					+ inParent.getDomainId();
			LOGGER.error(msg);
			throw new UnsupportedOperationException(msg);
		}
	}

	public UomMaster createUomMaster(String inDomainId) {
		UomMaster uomMaster = new UomMaster();
		uomMaster.setDomainId(inDomainId);
		this.addUomMaster(uomMaster);
		return uomMaster;
	}

	public ItemMaster createItemMaster(String inDomainId, String description, UomMaster uomMaster) {
		ItemMaster itemMaster = null;
		if (uomMaster.getParent().equals(this)) {
			itemMaster = new ItemMaster();
			itemMaster.setDomainId(inDomainId);
			itemMaster.setDescription(description);
			itemMaster.setStandardUom(uomMaster);
			this.addItemMaster(itemMaster);
		} else {
			LOGGER.error("can't create ItemMaster " + inDomainId + " with UomMaster " + uomMaster.getDomainId() + " under "
					+ this.getDomainId() + " because UomMaster parent is " + uomMaster.getParentFullDomainId());
		}
		return itemMaster;
	}

	public Aisle getAisle(String domainId) {
		Location location = this.getLocations().get(domainId);

		if (location != null) {
			if (location.getClass().equals(Aisle.class)) {
				return (Aisle) location;
			} else {
				LOGGER.error("child location " + domainId + " of Facility was not an Aisle, found " + location.getClassName());
			}
		} //else
		return null;
	}

	@Override
	public String toString() {
		return getDomainId();
	}
}
