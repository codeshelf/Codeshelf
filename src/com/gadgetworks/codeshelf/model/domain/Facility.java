/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.39 2012/11/03 23:57:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
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
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.PathDirectionEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@CacheStrategy
@ToString
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Facility extends LocationABC<Organization> {

	@Inject
	public static ITypedDao<Facility>	DAO;

	@Singleton
	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}
	}

	private static final Log			LOGGER			= LogFactory.getLog(Facility.class);

	//	// The owning organization.
	//	@Column(nullable = false)
	//	@ManyToOne(optional = false)
	//	@Getter
	//	private Organization				parentOrganization;

	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility					parent;

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Getter
	private List<Aisle>					aisles			= new ArrayList<Aisle>();

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@MapKey(name = "domainId")
	@Getter
	private Map<String, Container>		containers		= new HashMap<String, Container>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@MapKey(name = "domainId")
	@Getter
	private Map<String, ContainerKind>	containerKinds	= new HashMap<String, ContainerKind>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, targetEntity = DropboxService.class)
	@Getter
	private List<IEdiService>			ediServices		= new ArrayList<IEdiService>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Getter
	private List<ItemMaster>			itemMasters		= new ArrayList<ItemMaster>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Getter
	private List<OrderGroup>			orderGroups		= new ArrayList<OrderGroup>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Getter
	private List<OrderHeader>			orderHeaders	= new ArrayList<OrderHeader>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Getter
	private List<CodeShelfNetwork>		networks		= new ArrayList<CodeShelfNetwork>();

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@MapKey(name = "domainId")
	@Getter
	private Map<String, UomMaster>		uomMasters		= new HashMap<String, UomMaster>();

	public Facility() {
		orderHeaders = new ArrayList<OrderHeader>();
		containerKinds = new HashMap<String, ContainerKind>();
		containers = new HashMap<String, Container>();
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
	public final void createAisle(final String inAisleId,
		final Double inPosXMeters,
		final Double inPosYMeters,
		final Double inProtoBayXDimMeters,
		final Double inProtoBayYDimMeters,
		final Double inProtoBayZDimMeters,
		final Integer inBaysHigh,
		final Integer inBaysLong,
		final Boolean inRunInXDir,
		final Boolean inCreateBackToBack) {
		Aisle aisle = new Aisle(this, inAisleId, inPosXMeters, inPosYMeters);
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
				Bay protoBay = new Bay(aisle, bayId, anchorPosX, anchorPosY, anchorPosZ);
				try {
					Bay.DAO.store(protoBay);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}

				// Create the bay's boundary vertices.
				createVertices(protoBay, inProtoBayXDimMeters, inProtoBayYDimMeters);

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
		createAislePaths(aisle, aisleBoundaryX, aisleBoundaryY);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainId
	 * @param inPosTypeByStr
	 * @param inPosX
	 * @param inPosY
	 * @param inDrawOrder
	 */
	public final void createVertex(final String inDomainId, final String inPosTypeByStr, final Double inPosX, final Double inPosY, final Integer inDrawOrder) {

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
	private void createVertices(LocationABC inLocation, Double inXDimMeters, Double inYDimMeters) {
		try {
			// Create four simple vertices around the aisle.
			Vertex vertex1 = new Vertex(inLocation, "V01", 0, new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, null));
			Vertex.DAO.store(vertex1);
			Vertex vertex2 = new Vertex(inLocation, "V02", 1, new Point(PositionTypeEnum.METERS_FROM_PARENT, inXDimMeters, 0.0, null));
			Vertex.DAO.store(vertex2);
			Vertex vertex4 = new Vertex(inLocation, "V03", 2, new Point(PositionTypeEnum.METERS_FROM_PARENT, inXDimMeters, inYDimMeters, null));
			Vertex.DAO.store(vertex4);
			Vertex vertex3 = new Vertex(inLocation, "V04", 3, new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, inYDimMeters, null));
			Vertex.DAO.store(vertex3);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLocation
	 * @param inXDimMeters
	 * @param inYDimMeters
	 */
	private void createAislePaths(LocationABC inLocation, Double inXDimMeters, Double inYDimMeters) {

		Path path1 = new Path();
		path1.setParent(this);
		path1.setDomainId(getDefaultDomainIdPrefix() + inLocation.getDomainId());
		try {
			Path.DAO.store(path1);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		if (inXDimMeters < inYDimMeters) {
			// Create the "A" side path.
			Double xA = inLocation.getPosX() - inXDimMeters;
			Point headA = new Point(PositionTypeEnum.METERS_FROM_PARENT, xA, inLocation.getPosY(), null);
			Point tailA = new Point(PositionTypeEnum.METERS_FROM_PARENT, xA, inLocation.getPosY() + inYDimMeters, null);
			createPathSegment("A", inLocation, PathDirectionEnum.HEAD, path1, headA, tailA);

			// Create the "B" side path.
			Double xB = inLocation.getPosX() + inXDimMeters * 2.0;
			Point headB = new Point(PositionTypeEnum.METERS_FROM_PARENT, xB, inLocation.getPosY(), null);
			Point tailB = new Point(PositionTypeEnum.METERS_FROM_PARENT, xB, inLocation.getPosY() + inYDimMeters, null);
			createPathSegment("B", inLocation, PathDirectionEnum.TAIL, path1, headB, tailB);
		} else {
			// Create the "A" side path.
			Double yA = inLocation.getPosY() - inYDimMeters;
			Point headA = new Point(PositionTypeEnum.METERS_FROM_PARENT, inLocation.getPosX(), yA, null);
			Point tailA = new Point(PositionTypeEnum.METERS_FROM_PARENT, inLocation.getPosX() + inYDimMeters, yA, null);
			createPathSegment("A", inLocation, PathDirectionEnum.HEAD, path1, headA, tailA);

			// Create the "B" side path.
			Double yB = inLocation.getPosY() + inYDimMeters * 2.0;
			Point headB = new Point(PositionTypeEnum.METERS_FROM_PARENT, inLocation.getPosX(), yB, null);
			Point tailB = new Point(PositionTypeEnum.METERS_FROM_PARENT, inLocation.getPosX() + inYDimMeters, yB, null);
			createPathSegment("B", inLocation, PathDirectionEnum.TAIL, path1, headB, tailB);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPath
	 * @param inHead
	 * @param inTail
	 */
	private void createPathSegment(final String inSegmentId, final LocationABC inAssociatedLoc, final PathDirectionEnum inDirection, final Path inPath, final Point inHead, final Point inTail) {

		// The path segment goes along the longest segment of the aisle.
		PathSegment pathSegment = new PathSegment();
		pathSegment.setParent(inPath);
		pathSegment.setAssociatedLocation(inAssociatedLoc);
		pathSegment.setDirectionEnum(inDirection);
		pathSegment.setDomainId(inAssociatedLoc.getDomainId() + "." + pathSegment.getDefaultDomainIdPrefix() + inSegmentId);
		pathSegment.setHeadPoint(inHead);
		pathSegment.setTailPoint(inTail);
		try {
			PathSegment.DAO.store(pathSegment);
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
	public final void createDefaultContainerKind() {
		ContainerKind containerKind = createContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND, 0.0, 0.0, 0.0);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final ContainerKind createContainerKind(String inDomainId, Double inLengthMeters, Double inWidthMeters, Double inHeightMeters) {

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

}
