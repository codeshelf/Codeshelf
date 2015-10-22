/*******************************************************************************

 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import lombok.Getter;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.IEdiExportGateway;
import com.codeshelf.edi.IEdiGateway;
import com.codeshelf.edi.IEdiImportGateway;
import com.codeshelf.edi.WorkInstructionCsvBean;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.EdiGatewayStateEnum;
import com.codeshelf.model.HeaderCounts;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.DropboxGateway.DropboxCredentials;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.PropertyService;
import com.codeshelf.util.UomNormalizer;
import com.codeshelf.ws.protocol.message.DisconnectSiteControllerMessage;
import com.codeshelf.ws.server.WebSocketManagerService;
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

	private static final String	SFTPWIS_DOMAINID	= "SFTPWIS";

	private static final String	UNSPECIFIED_LOCATION_DOMAINID	= "FACILITY_UNSPECIFIED";

	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		@Override
		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}

		public Facility findByDomainId(final String domainId) {
			return super.findByDomainId(null, domainId);
		}
	}

	private static final Logger				LOGGER			= LoggerFactory.getLogger(Facility.class);

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, ContainerKind>		containerKinds	= new HashMap<String, ContainerKind>();

	@OneToMany(mappedBy = "parent", targetEntity = EdiGateway.class, orphanRemoval = true)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Getter
	private List<IEdiGateway>				ediGateways		= new ArrayList<IEdiGateway>();

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	@MapKey(name = "domainId")
	private Map<String, CodeshelfNetwork>	networks		= new HashMap<String, CodeshelfNetwork>();

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	@MapKey(name = "domainId")
	private Map<String, OrderGroup>			orderGroups		= new HashMap<String, OrderGroup>();

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	@MapKey(name = "domainId")
	private Map<String, Path>				paths			= new HashMap<String, Path>();

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	@MapKey(name = "domainId")
	private Map<String, UomMaster>			uomMasters		= new HashMap<String, UomMaster>();

	@OneToMany(mappedBy = "facility", orphanRemoval = true)
	private List<Worker>					workers;

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	private List<ImportReceipt>				dataImportReceipts;

	@OneToMany(mappedBy = "facility", orphanRemoval = true)
	private List<WorkerEvent>				workerEvents;

	@OneToMany(mappedBy = "facility", orphanRemoval = true)
	private List<Resolution>				resolutions;
	
	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	private List<ExportMessage>				exportMessages;

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	private List<WorkInstructionCsvBean>	workInstrcutionBeans;

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

	public void addEdiGateway(IEdiGateway inEdiGateway) {
		Facility previousFacility = inEdiGateway.getParent();
		if (previousFacility == null) {
			ediGateways.add(inEdiGateway);
			inEdiGateway.setParent(this);
		} else if (!previousFacility.equals(this)) {
			LOGGER.error("cannot add EdiGateway " + inEdiGateway.getServiceName() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousFacility.getDomainId());
		}
	}

	public void removeEdiGateway(IEdiGateway inEdiGateway) {
		if (this.ediGateways.contains(inEdiGateway)) {
			inEdiGateway.setParent(null);
			ediGateways.remove(inEdiGateway);
		} else {
			LOGGER.error("cannot remove EdiGateway " + inEdiGateway.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public List<UomMaster> getUomMasters() {
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

	/*
	 * Find the existing UOM that matches via normalization
	 * Note: may not be the normalized value. This is historical. First one wins.
	 */
	public UomMaster getNormalizedUomMaster(String inUomMasterId) {
		UomMaster foundMaster = getUomMaster(inUomMasterId);
		if (foundMaster == null) {
			List<UomMaster> masters = this.getUomMasters();
			for (UomMaster master : masters) {
				if (UomNormalizer.normalizedEquals(inUomMasterId, master.getUomMasterId())){
					foundMaster = master;
					break;
				}
			}
		}
		return foundMaster;
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
		if (list.isEmpty()) {
			return null;
		} else {
			return list.getFirst().getDomainId();
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
			for (SiteController siteController : siteControllers) {
				if (siteController.getDomainId().equals(String.valueOf(siteControllerId))) {
					foundSiteController = siteController;
				} else {
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
		location.setFirstLedNumAlongPath((short) 0);
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
	public ContainerKind createContainerKind(String inDomainId, Double inLengthMeters, Double inWidthMeters, Double inHeightMeters) {

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
	public IEdiExportGateway getEdiExportGateway() {
		IEdiExportGateway gateway = (SftpWiGateway)EdiGateway.staticGetDao().findByDomainId(this, SFTPWIS_DOMAINID);
		if (gateway != null && gateway.isLinked()) {
			return gateway;
		} else {
			return null;
		}
	}


	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public DropboxGateway getDropboxGateway() {
		DropboxGateway result = null;

		for (IEdiGateway ediGateway : getEdiGateways()) {
			if (Hibernate.getClass(ediGateway).equals(DropboxGateway.class)) {
				result = (DropboxGateway) ediGateway;
				break;
			}
		}
		return result;
	}

	public void createDefaultEDIGateways() {
		// Create a first Dropbox Service entry for this facility.
		@SuppressWarnings("unused")
		DropboxGateway dropboxGateway = createDropboxGateway();

		// Create a first IronMQ Service entry for this facility.
		try {
			@SuppressWarnings("unused")
			IronMqGateway ironMqGateway = createIronMqGateway();
		} catch (PSQLException e) {
			LOGGER.error("failed to create ironMQ service");
		}

		storeSftpService(new SftpWiGateway(SFTPWIS_DOMAINID));
		storeSftpService(new SftpOrderGateway("SFTPORDERS"));
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private DropboxGateway createDropboxGateway() {

		DropboxGateway result = null;

		result = new DropboxGateway();
		result.setDomainId("DROPBOX");
		result.setGatewayState(EdiGatewayStateEnum.UNLINKED);

		this.addEdiGateway(result);
		try {
			DropboxGateway.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private IronMqGateway createIronMqGateway() throws PSQLException {
		// we saw the PSQL exception in staging test when the record could not be added
		IronMqGateway result = null;

		result = new IronMqGateway();
		result.setDomainId("IRONMQ");
		result.setGatewayState(EdiGatewayStateEnum.UNLINKED);
		this.addEdiGateway(result);
		result.storeCredentials("", "", "true"); // non-null credentials
		try {
			IronMqGateway.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("Failed to save IronMQ service", e);
		}

		return result;
	}


	private void storeSftpService(SftpGateway sftpEDI) {
		this.addEdiGateway(sftpEDI);
		try {
			sftpEDI.getDao().store(sftpEDI);
		} catch (DaoException e) {
			LOGGER.error("Failed to save {} service", sftpEDI.getClass().toString(), e);
		}


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
		// FIXME: replace with database query instead of cycling through all item masters
		List<ItemMaster> allItemMasters = ItemMaster.staticGetDao().findByParent(this);
		List<ItemMaster> ddcItemMasters = new ArrayList<ItemMaster>();
		for (ItemMaster itemMaster : allItemMasters) {
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
		List<OrderHeader> orderHeaders = OrderHeader.staticGetDao().findByParent(this);
		if (orderHeaders != null) {
			for (OrderHeader order : orderHeaders) {
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
		Aisle aisle = Aisle.staticGetDao().findByDomainId(this, inAisleId);
		if (aisle == null) {
			aisle = new Aisle();
			aisle.setDomainId(inAisleId);
			aisle.setAnchorPoint(inAnchorPoint);
			aisle.setPickFaceEndPoint(inPickFaceEndPoint);

			this.addAisle(aisle);
		}
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
		UomMaster uomMaster = UomMaster.staticGetDao().findByDomainId(this, inDomainId);
		if (uomMaster == null) {
			uomMaster = new UomMaster();
			uomMaster.setDomainId(inDomainId);
			this.addUomMaster(uomMaster);
		}
		return uomMaster;
	}

	public ItemMaster createItemMaster(String inDomainId, String description, UomMaster uomMaster) {
		ItemMaster itemMaster = null;
		if (uomMaster.getParent().equals(this)) {
			itemMaster = new ItemMaster(this, inDomainId, uomMaster);
			itemMaster.setDescription(description);
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

	synchronized public Location getUnspecifiedLocation() {
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

		LOGGER.info("Creating facility " + inDomainId
				+ " w/ all edi services, network, sitecon, sitecon user, generic container and 2 CHEs");

		facility.createDefaultEDIGateways();
		// Create the default network for the facility.
		CodeshelfNetwork network = facility.createNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);

		// Create a site controller & associated user
		int siteconSerial = CodeshelfNetwork.DEFAULT_SITECON_SERIAL;
		while (SiteController.staticGetDao().findByDomainId(null, Integer.toString(siteconSerial)) != null) {
			// pick first available site controller serial number e.g. 5001
			siteconSerial++;
		}
		network.createSiteController(siteconSerial, "Default Area", false);

		// Create the generic container kind (for all unspecified containers)
		facility.createDefaultContainerKind();

		// Setup two dummy CHEs
		int cheNum = 0;
		for (int chesCreated = 0; chesCreated < 2; chesCreated++) {
			String cheName;
			do {
				cheNum++;
				cheName = "CHE" + cheNum;
			} while (Che.staticGetDao().findByDomainId(null, cheName) != null);
			@SuppressWarnings("unused")
			Che che = network.createChe(cheName, new NetGuid(String.format("0x%08X", 0x9990 + cheNum)));
		}

		return facility;
	}

	// convenience method
	public Facility reload() {
		return Facility.staticGetDao().reload(this);
	}

	/**
	 * Deletes all Facilities in the current schema. Use carefully.
	 */
	public static void deleteAll(WebSocketManagerService webSocketManagerService) {
		TenantPersistenceService persistence = TenantPersistenceService.getInstance(); // convenience
		String schema = CodeshelfSecurityManager.getCurrentTenant().getSchemaName();
		Session session = persistence.getSession();
		List<Facility> facilities = Facility.staticGetDao().getAll();
		for (Facility facility : facilities) {
			Set<User> users = facility.getSiteControllerUsers();
			DisconnectSiteControllerMessage disconnectMessage = new DisconnectSiteControllerMessage();
			webSocketManagerService.sendMessage(users, disconnectMessage);
		}
		//The "location" table contains the Facility. All objects we want to delete are descendants of the Facility
		String query = String.format("TRUNCATE %s.location CASCADE", schema);
		session.createSQLQuery(query).executeUpdate();
	}

	public void delete(WebSocketManagerService webSocketManagerService) {
		Set<User> users = getSiteControllerUsers();
		delete();
		DisconnectSiteControllerMessage disconnectMessage = new DisconnectSiteControllerMessage();
		webSocketManagerService.sendMessage(users, disconnectMessage);
	}

	public void delete() {
		//See https://codeshelf.atlassian.net/wiki/pages/viewpage.action?pageId=24936525 (CD_0096) for details
		//Step 1 - WorkInstruction
		List<WorkInstruction> workInstructions = WorkInstruction.staticGetDao().findByParent(this);
		deleteCollection(workInstructions, WorkInstruction.staticGetDao());
		
		//Step 2 - OrderHeader, ContainerUse, OrderDetail, OrderLocation
		List<OrderHeader> orderHeaders = OrderHeader.staticGetDao().findByParent(this);
		deleteCollection(orderHeaders, OrderHeader.staticGetDao());

		//Step 3 - ContainerKind, Container
		deleteCollection(containerKinds.values(), ContainerKind.staticGetDao());

		//Step 4 - ItemMaster, Item, Gtin
		List<ItemMaster> itemMasters = ItemMaster.staticGetDao().findByParent(this);
		deleteCollection(itemMasters, ItemMaster.staticGetDao());

		//Step 4 - ExtensionPoint
		List<ExtensionPoint> extensionPoints = ExtensionPoint.staticGetDao().findByParent(this);
		deleteCollection(extensionPoints, ExtensionPoint.staticGetDao());

		//Step 6 - Facility and remaining linked domain object
		Facility.staticGetDao().delete(this);
	}

	private void deleteCollection(Collection<?> collection, ITypedDao<?> dao) {
		if (collection == null || collection.isEmpty()) {
			return;
		}
		for (Object object : collection) {
			dao.delete((IDomainObject) object);
		}
	}
	
	public List<IEdiImportGateway> getLinkedEdiImportGateways() {
		//TODO only return services that implement import interface
		ArrayList<IEdiImportGateway> importServices = new ArrayList<>();
		for (IEdiGateway ediGateway : getEdiGateways()) {
			if (ediGateway instanceof IEdiImportGateway && ediGateway.isLinked()) {
				importServices.add((IEdiImportGateway)ediGateway);
			}
		}
		return importServices;
	}

	@SuppressWarnings("unchecked")
	public <T extends IEdiGateway> T findEdiGateway(Class<T> cls) {
		for (IEdiGateway ediGateway : getEdiGateways()) {
			if (cls.isAssignableFrom(ediGateway.getClass())) {
				return (T) ediGateway;
			}
		}
		return null;
	}
	
	public IEdiGateway findEdiGateway(String domainId) {
		for (IEdiGateway ediGateway : getEdiGateways()) {
			if (ediGateway.getDomainId().equals(domainId)) {
				return ediGateway;
			}
		}
		return null;
	}
	
	public TimeZone getTimeZone(){
		return TimeZone.getTimeZone("US/Mountain");
	}
	
	@Transient
	private boolean dropboxLegacyCleanupDone = false;
	
	/**
	 * This function deals with pre-v22  way of storing Dropbox credentials.
	 * The authentication key used to be stored as a string instead of a Json object. 
	 * This function corrects that.
	 */
	public void dropboxLegacyCredentialsCleanup(){
		if (!dropboxLegacyCleanupDone){
			List<EdiGateway> ediGateways = EdiGateway.staticGetDao().findByParent(this);
			for (IEdiGateway ediGateway : ediGateways){
				if (ediGateway instanceof DropboxGateway) {
					DropboxGateway dropboxGateway = (DropboxGateway)ediGateway;
					String credentials = dropboxGateway.getProviderCredentials();
					if (credentials == null || !credentials.startsWith("{")){
						dropboxGateway.setProviderCredentials(new DropboxCredentials(credentials).toString());
					}
				}
			}
			dropboxLegacyCleanupDone = true;
		}
	}
	
	public FacilityMetric getMetrics(String dateStr) throws Exception{
		Calendar cal = getDateForMetrics(dateStr, getTimeZone());
		Timestamp metricsCollectionStartUTC = new Timestamp(cal.getTimeInMillis());
		FacilityMetric metric = getMetrics(metricsCollectionStartUTC, false);
		return metric;
	}
	
	public void computeMetrics(String dateStr) throws Exception{
		TimeZone facilityTimeZone = getTimeZone();
		SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		outFormat.setTimeZone(facilityTimeZone);
		Calendar cal = getDateForMetrics(dateStr, facilityTimeZone);
		String dateLocalUI = outFormat.format(cal.getTime());
		Timestamp metricsCollectionStartUTC = new Timestamp(cal.getTimeInMillis());
		cal.add(Calendar.DATE, 1);
		Timestamp metricsCollectionEndUTC = new Timestamp(cal.getTimeInMillis());
		
		FacilityMetric metric = getMetrics(metricsCollectionStartUTC, true);
		metric.setUpdated(new Timestamp(System.currentTimeMillis()));
		metric.setTz(cal.getTimeZone().getID());
		metric.setDateLocalUI(dateLocalUI);
		metric.setDomainId(metric.getDefaultDomainIdPrefix() + "-" + getDomainId() + "-" + metric.getDateLocalUI());
		
		int ordersPickedCalculated = computeOrderMetrics(metric, metricsCollectionStartUTC, metricsCollectionEndUTC);
		int ordersPickedOld = metric.getOrdersPicked();
		if (ordersPickedCalculated < ordersPickedOld / 10){
			return;
		}
		metric.setOrdersPicked(ordersPickedCalculated);
		computeDetailMetrics(metric, metricsCollectionStartUTC, metricsCollectionEndUTC);
		computeHousekeepingMetrics(metric, metricsCollectionStartUTC, metricsCollectionEndUTC);
		computeEventMetrics(metric, metricsCollectionStartUTC, metricsCollectionEndUTC);
		FacilityMetric.staticGetDao().store(metric);
	}
	
	private Calendar getDateForMetrics(String dateStr, TimeZone facilityTimeZone) throws Exception{
		Calendar cal = Calendar.getInstance(facilityTimeZone);
		if (dateStr != null) {
			SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
			dayFormat.setTimeZone(facilityTimeZone);
			Date date = dayFormat.parse(dateStr);
			cal.setTime(date);
		}
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	private int computeOrderMetrics(FacilityMetric metric, Timestamp startUtc, Timestamp endUtc){
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", this));
		filterParams.add(Restrictions.eq("status", OrderStatusEnum.COMPLETE));
		filterParams.add(Restrictions.ge("updated", startUtc));
		filterParams.add(Restrictions.le("updated", endUtc));
		int ordersPicked = OrderHeader.staticGetDao().countByFilter(filterParams);
		return ordersPicked;
				
	}
	
	private void computeDetailMetrics(FacilityMetric metric, Timestamp startUtc, Timestamp endUtc){
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("status", OrderStatusEnum.COMPLETE));
		filterParams.add(Restrictions.ge("updated", startUtc));
		filterParams.add(Restrictions.le("updated", endUtc));
		List<OrderDetail> details = OrderDetail.staticGetDao().findByFilter(filterParams);
		int linesTotal = 0, linesEach = 0, linesCase = 0, linesOther = 0;
		int countTotal = 0, countEach = 0, countCase = 0, countOther = 0;
		for (OrderDetail detail : details) {
			if (!this.equals(detail.getFacility())){
				continue;
			}
			int actual = detail.getActualPickedItems();
			if (actual == 0) {
				continue;
			}
			linesTotal++;
			countTotal += actual;
			String uomId = detail.getUomMasterId();
			if (UomNormalizer.isEach(uomId)){
				linesEach++;
				countEach += actual;
			} else if (UomNormalizer.isCase(uomId)){
				linesCase++;
				countCase += actual;
			} else {
				linesOther++;
				countOther += actual;
			}
		}
		metric.setLinesPicked(linesTotal);
		metric.setLinesPickedEach(linesEach);
		metric.setLinesPickedCase(linesCase);
		metric.setLinesPickedOther(linesOther);
		metric.setCountPicked(countTotal);
		metric.setCountPickedEach(countEach);
		metric.setCountPickedCase(countCase);
		metric.setCountPickedOther(countOther);
	}
	
	private void computeHousekeepingMetrics(FacilityMetric metric, Timestamp startUtc, Timestamp endUtc){
		List<WorkInstructionTypeEnum> housekeepingTypes = new ArrayList<>();
		housekeepingTypes.add(WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		housekeepingTypes.add(WorkInstructionTypeEnum.HK_REPEATPOS);
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", this));
		filterParams.add(Restrictions.in("type", housekeepingTypes));
		filterParams.add(Restrictions.eq("status", OrderStatusEnum.COMPLETE));
		filterParams.add(Restrictions.ge("completed", startUtc));
		filterParams.add(Restrictions.le("completed", endUtc));
		int housekeepingInstructionsCount = WorkInstruction.staticGetDao().countByFilter(filterParams);
		metric.setHouseKeeping(housekeepingInstructionsCount);		
	}
	
	private void computeEventMetrics(FacilityMetric metric, Timestamp startUtc, Timestamp endUtc){
		List<EventType> shortEventTypes = new ArrayList<>();
		shortEventTypes.add(EventType.SHORT);
		shortEventTypes.add(EventType.SHORT_AHEAD);
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", this));
		filterParams.add(Restrictions.in("eventType", shortEventTypes));
		filterParams.add(Restrictions.ge("created", startUtc));
		filterParams.add(Restrictions.le("created", endUtc));
		int shortEventsCount = WorkerEvent.staticGetDao().countByFilter(filterParams);
		metric.setShortEvents(shortEventsCount);
		
		filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", this));
		filterParams.add(Restrictions.eq("eventType", EventType.SKIP_ITEM_SCAN));
		filterParams.add(Restrictions.ge("created", startUtc));
		filterParams.add(Restrictions.le("created", endUtc));
		int skipEventsCount = WorkerEvent.staticGetDao().countByFilter(filterParams);
		metric.setSkipScanEvents(skipEventsCount);
	}
	
	private FacilityMetric getMetrics(Timestamp date, boolean createNewIfNeeded){
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", this));
		filterParams.add(Restrictions.eq("date", date));
		List<FacilityMetric> metrics = FacilityMetric.staticGetDao().findByFilter(filterParams);
		FacilityMetric metric = null;
		if (metrics.isEmpty()) {
			if (createNewIfNeeded) {
				metric = new FacilityMetric();
				metric.setParent(this);
				metric.setDate(date);
			} else {
				return null;
			}
		} else if (metrics.size() == 1){
			metric = metrics.get(0);
		} else {
			LOGGER.warn("Found more than one FacilityMertic for facility " + getDomainId() + " at " + date + ". Using latest one.");
			Timestamp latestTs = new Timestamp(0);
			for (FacilityMetric metricCheck : metrics) {
				if (metricCheck.getUpdated().after(latestTs)){
					metric = metricCheck;
				}
			}
		}
		return metric;
	}
}