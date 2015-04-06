/*******************************************************************************

 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

import lombok.Getter;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.model.EdiProviderEnum;
import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.HeaderCounts;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.PropertyService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

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
	private static final String 		UNSPECIFIED_LOCATION_DOMAINID	= "FACILITY_UNSPECIFIED";

	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
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
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
	
	public List<Path> getPaths() {
		return new ArrayList<Path>(this.paths.values());
	}

	@Override
	public final String getDefaultDomainIdPrefix() {
		return "F";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<Facility> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<Facility> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Facility.class);
	}

	@Override
	public String getFullDomainId() {
		return getDomainId();
	}

	public void setFacilityId(String inFacilityId) {
		setDomainId(inFacilityId);
	}

	public void addAisle(Aisle inAisle) {
		Location previousFacility = inAisle.getParent();
		if (previousFacility == null) {
			this.addLocation(inAisle);
			inAisle.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add Aisle " + inAisle.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public void removeAisle(Aisle inAisle) {
		String domainId = inAisle.getDomainId();
		if (this.getLocations().get(domainId) != null) {
			inAisle.setParent(null);
			this.removeLocation(domainId);
		} else {
			LOGGER.error("cannot remove Aisle " + inAisle.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public void addPath(Path inPath) {
		Facility facility = inPath.getParent();
		if (facility != null && !facility.equals(this)) {
			LOGGER.error("cannot add Path " + inPath.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + facility.getDomainId());
			return;
		}
		paths.put(inPath.getDomainId(), inPath);
		inPath.setParent(this);
	}

	public Path getPath(String inPathId) {
		return paths.get(inPathId);
	}

	public void removePath(String inPathId) {
		Path path = this.getPath(inPathId);
		if (path != null) {
			path.setParent(null);
			paths.remove(inPathId);
		} else {
			LOGGER.error("cannot remove Path " + inPathId + " from " + this.getDomainId() + " because it isn't found in children");
		}
	}

	public void addContainer(Container inContainer) {
		Facility previousFacility = inContainer.getParent();
		if (previousFacility == null) {
			containers.put(inContainer.getDomainId(), inContainer);
			inContainer.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add Container " + inContainer.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public Container getContainer(String inContainerId) {
		return containers.get(inContainerId);
	}

	public List<Container> getContainers() {
		return new ArrayList<Container>(containers.values());
	}

	public void removeContainer(String inContainerId) {
		Container container = this.getContainer(inContainerId);
		if (container != null) {
			container.setParent(null);
			containers.remove(inContainerId);
		} else {
			LOGGER.error("cannot remove Container " + inContainerId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public void addContainerKind(ContainerKind inContainerKind) {
		Facility previousFacility = inContainerKind.getParent();
		if (previousFacility == null) {
			containerKinds.put(inContainerKind.getDomainId(), inContainerKind);
			inContainerKind.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add ContainerKind " + inContainerKind.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public ContainerKind getContainerKind(String inContainerKindId) {
		return containerKinds.get(inContainerKindId);
	}

	public void removeContainerKind(String inContainerKindId) {
		ContainerKind containerKind = this.getContainerKind(inContainerKindId);
		if (containerKind != null) {
			containerKind.setParent(null);
			containerKinds.remove(inContainerKindId);
		} else {
			LOGGER.error("cannot remove ContainerKind " + inContainerKindId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public void addEdiService(IEdiService inEdiService) {
		Facility previousFacility = inEdiService.getParent();
		if (previousFacility == null) {
			ediServices.add(inEdiService);
			inEdiService.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add EdiService " + inEdiService.getServiceName() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public void removeEdiService(IEdiService inEdiService) {
		if (this.ediServices.contains(inEdiService)) {
			inEdiService.setParent(null);
			ediServices.remove(inEdiService);
		} else {
			LOGGER.error("cannot remove EdiService " + inEdiService.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public void addWorkInstruction(WorkInstruction wi) {
		Facility previousFacility = wi.getParent();
		if (previousFacility == null) {
			getWorkInstructions().add(wi);
			wi.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add WorkInstruction " + wi.getPersistentId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId(), new Exception());
		}
	}

	public void removeWorkInstruction(WorkInstruction wi) {
		if (this.workInstructions.contains(wi)) {
			wi.setParent(null);
			workInstructions.remove(wi);
		} else {
			LOGGER.error("cannot remove WorkInstruction " + wi.getPersistentId() + " from " + this.getDomainId()
					+ " because it isn't found in children", new Exception());
		}
	}

	public void addItemMaster(ItemMaster inItemMaster) {
		Facility previousFacility = inItemMaster.getParent();
		if (previousFacility == null) {
			itemMasters.put(inItemMaster.getDomainId(), inItemMaster);
			inItemMaster.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add ItemMaster " + inItemMaster.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId(), new Exception());
		}
	}

	public ItemMaster getItemMaster(String inItemMasterId) {
		return itemMasters.get(inItemMasterId);
	}

	public void removeItemMaster(String inItemMasterId) {
		ItemMaster itemMaster = this.getItemMaster(inItemMasterId);
		if (itemMaster != null) {
			itemMaster.setParent(null);
			itemMasters.remove(inItemMasterId);
		} else {
			LOGGER.error("cannot remove ItemMaster " + inItemMasterId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public  List<ItemMaster> getItemMasters() {
		return new ArrayList<ItemMaster>(itemMasters.values());
	}

	public  List<UomMaster> getUomMasters() {
		return new ArrayList<UomMaster>(uomMasters.values());
	}

	public void addOrderGroup(OrderGroup inOrderGroup) {
		Facility previousFacility = inOrderGroup.getParent();
		if (previousFacility == null) {
			orderGroups.put(inOrderGroup.getDomainId(), inOrderGroup);
			inOrderGroup.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add OrderGroup " + inOrderGroup.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public OrderGroup getOrderGroup(String inOrderGroupId) {
		return orderGroups.get(inOrderGroupId);
	}

	public void removeOrderGroup(String inOrderGroupId) {
		OrderGroup orderGroup = this.getOrderGroup(inOrderGroupId);
		if (orderGroup != null) {
			orderGroup.setParent(null);
			orderGroups.remove(inOrderGroupId);
		} else {
			LOGGER.error("cannot remove OrderGroup " + inOrderGroupId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public List<OrderGroup> getOrderGroups() {
		return new ArrayList<OrderGroup>(orderGroups.values());
	}

	public void addOrderHeader(OrderHeader inOrderHeader) {
		Facility previousFacility = inOrderHeader.getParent();
		if (previousFacility != null && !previousFacility.equals(this)) {
			LOGGER.error("cannot add OrderHeader " + inOrderHeader.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		} else {
			orderHeaders.put(inOrderHeader.getDomainId(), inOrderHeader);
			inOrderHeader.setParent(this);
		}
	}

	public OrderHeader getOrderHeader(String inOrderHeaderId) {
		return orderHeaders.get(inOrderHeaderId);
	}

	public void removeOrderHeader(String inOrderHeaderId) {
		OrderHeader orderHeader = this.getOrderHeader(inOrderHeaderId);
		if (orderHeader != null) {
			orderHeader.setParent(null);
			orderHeaders.remove(inOrderHeaderId);
		} else {
			LOGGER.error("cannot remove OrderHeader " + inOrderHeaderId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public List<OrderHeader> getOrderHeaders() {
		return new ArrayList<OrderHeader>(orderHeaders.values());
	}

	public void addUomMaster(UomMaster inUomMaster) {
		Facility previousFacility = inUomMaster.getParent();
		if (previousFacility == null) {
			uomMasters.put(inUomMaster.getDomainId(), inUomMaster);
			inUomMaster.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add UomMaster " + inUomMaster.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public UomMaster getUomMaster(String inUomMasterId) {
		return uomMasters.get(inUomMasterId);
	}

	public void removeUomMaster(String inUomMasterId) {
		UomMaster uomMaster = this.getUomMaster(inUomMasterId);
		if (uomMaster != null) {
			uomMaster.setParent(null);
			uomMasters.remove(inUomMasterId);
		} else {
			LOGGER.error("cannot remove UomMaster " + inUomMasterId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public void addNetwork(CodeshelfNetwork inNetwork) {
		Facility previousFacility = inNetwork.getParent();
		if (previousFacility == null) {
			networks.put(inNetwork.getDomainId(), inNetwork);
			inNetwork.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add CodeshelfNetwork " + inNetwork.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public CodeshelfNetwork getNetwork(String inNetworkId) {
		return networks.get(inNetworkId);
	}

	public void removeNetwork(String inNetworkId) {
		CodeshelfNetwork network = this.getNetwork(inNetworkId);
		if (network != null) {
			network.setParent(null);
			networks.remove(inNetworkId);
		} else {
			LOGGER.error("cannot remove CodeshelfNetwork " + inNetworkId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public List<CodeshelfNetwork> getNetworks() {
		return new ArrayList<CodeshelfNetwork>(networks.values());
	}

	public void addLocationAlias(LocationAlias inLocationAlias) {
		Facility previousFacility = inLocationAlias.getParent();
		if (previousFacility == null) {

			locationAliases.put(inLocationAlias.getDomainId(), inLocationAlias);
			inLocationAlias.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add LocationAlias " + inLocationAlias.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public LocationAlias getLocationAlias(String inLocationAliasId) {
		return locationAliases.get(inLocationAliasId);
	}

	public void removeLocationAlias(LocationAlias inLocationAlias) {
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

	public List<LocationAlias> getLocationAliases() {
		return new ArrayList<LocationAlias>(locationAliases.values());
	}

	@Override
	public Point getAbsoluteAnchorPoint() {
		return Point.getZeroPoint();
	}

	public Double getAbsolutePosX() {
		return 0.0;
	}

	public Double getAbsolutePosY() {
		return 0.0;
	}

	public Double getAbsolutePosZ() {
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

	@JsonProperty("primarySiteControllerId")
	public String getPrimarySiteControllerId() {
		CodeshelfNetwork network = this.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		Collection<SiteController> siteControllers = network.getSiteControllers().values();
		if (siteControllers.size() > 1) {
			LOGGER.warn("Multiple site controllers found but expected no more than one for facility: {}", this);
		} 
		LinkedList<SiteController> list = new LinkedList<>(siteControllers);
		SiteController first = list.getFirst();
		if (first != null) {
			return first.getDomainId();
		} else {
			return null;
		}
	}

	@JsonProperty("primarySiteControllerId")
	public void setPrimarySiteControllerId(String siteControllerId) {
		if (!Strings.isNullOrEmpty(siteControllerId)) {
			CodeshelfNetwork network = this.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
			Collection<SiteController> siteControllers = network.getSiteControllers().values();
			if (siteControllers.size() > 1) {
				LOGGER.warn("Multiple site controllers found but expected no more than one for facility: {}", this);
			}
			
			SiteController foundSiteController = null;
			for (SiteController siteController: siteControllers) {
				if(siteController.getDomainId().equals(String.valueOf(siteControllerId))) {
					foundSiteController = siteController;
				} 
				else {
					network.removeSiteController(siteController.getDomainId());
					SiteController.staticGetDao().delete(siteController);
				}
			}
			if (foundSiteController == null) {
				network.createSiteController(Integer.parseInt(siteControllerId), "Default Area", false);
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
			Path existingPath = Path.staticGetDao().findByDomainId(this, pathId);
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

	public Path createPath(String inDomainId) {
		Path path = new Path();
		// Start keeping the API, but not respecting the suggested domainId
		String pathDomainId = nextPathDomainId();
		if (!inDomainId.isEmpty() && !pathDomainId.equals(inDomainId)) {
			LOGGER.warn("revise createPath() caller or API");
		}
		path.setDomainId(pathDomainId);

		this.addPath(path);
		Path.staticGetDao().store(path);

		path.createDefaultWorkArea(); //TODO an odd way to construct, but it is a way to make sure the Path is persisted before the work area
		return path;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a path
	 *
	 */
	public Path createPath(String inDomainId, PathSegment[] inPathSegments) {
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
			PathSegment.staticGetDao().store(pathSegment);
		}
		return path;
	}

	// --------------------------------------------------------------------------
	/**
	 * A sample routine to show the distance of locations along a path.
	 */
	public void recomputeLocationPathDistances(Path inPath) {
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
	public void createVertex(final String inDomainId,
		final String inPosTypeByStr,
		final Double inPosX,
		final Double inPosY,
		final Integer inDrawOrder) {

		Vertex vertex = new Vertex();
		vertex.setDomainId(inDomainId);
		vertex.setPoint(new Point(PositionTypeEnum.valueOf(inPosTypeByStr), inPosX, inPosY, null));
		vertex.setDrawOrder(inDrawOrder);
		this.addVertex(vertex);

		Vertex.staticGetDao().store(vertex);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLocation
	 * @param inDimMeters
	 */
	public void createOrUpdateVertices(Location inLocation, Point inDimMeters) {
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
					Vertex.staticGetDao().store(vertexN);
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
						// Vertex.staticGetDao().delete(theVertex);
						LOGGER.error("extra vertex?. Why is it here?");
					} else {
						// just update the points
						// and verify the name
						String vertexName = theVertex.getDomainId();
						if (!vertexName.equals(vertexNames[n])) {
							LOGGER.error("Wrong vertex name. How?");
						}
						theVertex.setPoint(points[n]);
						Vertex.staticGetDao().store(theVertex);
					}
				}

			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	private Location createUnspecifiedLocation(String domainId) {
		UnspecifiedLocation location = new UnspecifiedLocation(domainId);
		location.setFirstLedNumAlongPath((short)0);
		this.addLocation(location);
		UnspecifiedLocation.staticGetDao().store(location);
		return location;
	}
	
	// --------------------------------------------------------------------------
	/**
	 */
	public void createDefaultContainerKind() {
		//ContainerKind containerKind = 
		createContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND, 0.0, 0.0, 0.0);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public ContainerKind createContainerKind(String inDomainId,
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
			ContainerKind.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public IEdiService getEdiExportService() {
		return IronMqService.staticGetDao().findByDomainId(this, IRONMQ_DOMAINID);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public IronMqService createIronMqService() throws PSQLException {
		// we saw the PSQL exception in staging test when the record could not be added
		IronMqService result = null;

		result = new IronMqService();
		result.setDomainId(IRONMQ_DOMAINID);
		result.setProvider(EdiProviderEnum.IRONMQ);
		result.setServiceState(EdiServiceStateEnum.UNLINKED);
		this.addEdiService(result);
		result.storeCredentials("", ""); // non-null credentials
		try {
			IronMqService.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("Failed to save IronMQ service", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public DropboxService getDropboxService() {
		DropboxService result = null;

		for (IEdiService ediService : getEdiServices()) {
			if (Hibernate.getClass(ediService).equals(DropboxService.class)) {
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
	public DropboxService createDropboxService() {

		DropboxService result = null;

		result = new DropboxService();
		result.setDomainId("DROPBOX");
		result.setProvider(EdiProviderEnum.DROPBOX);
		result.setServiceState(EdiServiceStateEnum.UNLINKED);

		this.addEdiService(result);
		try {
			DropboxService.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public CodeshelfNetwork createNetwork(final String inNetworkName) {

		CodeshelfNetwork result = null;

		result = new CodeshelfNetwork();
		result.setDomainId(inNetworkName);
		result.setActive(true);
		result.setDescription("");
		//result.setCredential(Double.toString(Math.random()));
		//result.setCredential("0.6910096026612129");

		this.addNetwork(result);

		try {
			CodeshelfNetwork.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("DaoException persistence error storing CodeshelfNetwork", e);
		}

		return result;
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
	public void recomputeDdcPositions() {

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
				ItemDdcGroup.staticGetDao().delete(ddcGroup);
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
				Item.staticGetDao().store(item);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

			// Figure out if we've changed DDC group codes and start a new group.
			if ((lastDdcGroup == null) || (!lastDdcGroup.getDdcGroupId().equals(item.getParent().getDdcId()))) {

				// Finish the end position of the last DDC group and store it.
				if (lastDdcGroup != null) {
					ItemDdcGroup.staticGetDao().store(lastDdcGroup);
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
			ItemDdcGroup.staticGetDao().store(lastDdcGroup);
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
	public int countLedControllers() {
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
	public boolean hasCrossBatchOrders() {
		// DEV-582 ties this to the config parameter. Used to be inferred from the data
		String theValue = PropertyService.getInstance().getPropertyFromConfig(this, DomainObjectProperty.CROSSBCH);
		boolean result = Boolean.parseBoolean(theValue);
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * The UI needs this answer. UI gets it at login.
	 * If true, the UI wants to believe that ALL crossbatch and outbound orders have an order group. The orders view will not show at all any orders without a group.
	 */
	@JsonProperty("hasMeaningfulOrderGroups")
	public boolean hasMeaningfulOrderGroups() {
		// We really want to change to a config parameter, and then pass to the UI a three-value choice:
		// Only two-level Orders view, only three-level, or both 2 and 3-level.		

		List<OrderGroup> groupsList = this.getOrderGroups();
		boolean result = groupsList.size() > 0;
		// clearly might give the wrong value if site is initially misconfigured. Could look at the orderHeaders in more detail. Do most have groups?

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param outHeaderCounts
	 */
	public HeaderCounts countCrossOrders() {
		return countOrders(OrderTypeEnum.CROSS);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param outHeaderCounts
	 */
	public HeaderCounts countOutboundOrders() {

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
							inactiveDetailsOnActiveOrders++;
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

	private Set<SiteController> getSiteControllers() {
		Set<SiteController> siteControllers = new HashSet<SiteController>();

		for (CodeshelfNetwork network : this.getNetworks()) {
			siteControllers.addAll(network.getSiteControllers().values());
		}
		return siteControllers;
	}

	public Set<User> getSiteControllerUsers() {
		Set<User> users = new HashSet<User>();

		for (SiteController sitecon : this.getSiteControllers()) {
			User user = TenantManagerService.getInstance().getUser(sitecon.getDomainId());
			if (user != null) {
				users.add(user);
			} else {
				LOGGER.warn("Couldn't find user for site controller " + sitecon.getDomainId());
			}
		}
		return users;
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
			if (location.isAisle()) {
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

	synchronized
	public Location getUnspecifiedLocation() {
		Location unspecifiedLocation = this.getLocations().get(UNSPECIFIED_LOCATION_DOMAINID);
		if (unspecifiedLocation == null) {
			unspecifiedLocation = createUnspecifiedLocation(UNSPECIFIED_LOCATION_DOMAINID);
		}
		return unspecifiedLocation;
	}
	// --------------------------------------------------------------------------
	/**
	 * @param inDomainId
	 * @param inDescription
	 * @param inPosTypeByStr
	 * @param inAnchorPosx
	 * @param inAnchorPosY
	 */
	// @Transactional
	public static Facility createFacility(final String inDomainId, final String inDescription, final Point inAnchorPoint) {

		Facility facility = new Facility();
		facility.setDomainId(inDomainId);
		facility.setDescription(inDescription);
		facility.setAnchorPoint(inAnchorPoint);
		facility.store();

		LOGGER.info("Creating facility "+inDomainId+" w/ dropbox, ironmq, network, sitecon, sitecon user, generic container and 2 CHEs");

		// Create a first Dropbox Service entry for this facility.
		@SuppressWarnings("unused")
		DropboxService dropboxService = facility.createDropboxService();

		// Create a first IronMQ Service entry for this facility.
		try {
			@SuppressWarnings("unused")
			IronMqService ironMqService = facility.createIronMqService();
		}
		catch (PSQLException e) {
			LOGGER.error("failed to create ironMQ service");			
		}

		// Create the default network for the facility.
		CodeshelfNetwork network = facility.createNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		
		// Create a site controller & associated user
		network.createSiteController(CodeshelfNetwork.DEFAULT_SITECON_SERIAL, "Default Area", false);
		
		// Create the generic container kind (for all unspecified containers)
		facility.createDefaultContainerKind();
		
		// Setup two dummy CHEs
		for (int cheNum = 1; cheNum <= 2; cheNum++) {
			String cheName = "CHE" + cheNum;
			Che che = network.getChe(cheName);
			if (che == null) {
				che = network.createChe(cheName, new NetGuid("0x0000999" + cheNum));
			}
		}
		
		return facility;
	}

}
