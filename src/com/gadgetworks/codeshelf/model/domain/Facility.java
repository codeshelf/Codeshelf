/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.32 2012/10/29 02:59:26 jeffw Exp $
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
import javax.persistence.Table;

import lombok.Getter;
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
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
public class Facility extends LocationABC {

	@Inject
	public static ITypedDao<Facility>	DAO;

	@Singleton
	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}
	}

	private static final Log			LOGGER			= LogFactory.getLog(Facility.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Organization				parentOrganization;

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
		// Facilities have no parent location, but we don't want to allow ANY location to not have a parent.
		// So in this case we make the facility its own parent.  It's also a way to know when we've topped-out in the location tree.
		parent = this;
		orderHeaders = new ArrayList<OrderHeader>();
		containerKinds = new HashMap<String, ContainerKind>();
		containers = new HashMap<String, Container>();
	}

	public Facility(final Double inPosX, final Double inPosY) {
		super(PositionTypeEnum.GPS, inPosX, inPosY);
		// Facilities have no parent location, but we don't want to allow ANY location to not have a parent.
		// So in this case we make the facility its own parent.  It's also a way to know when we've topped-out in the location tree.
		parent = this;
	}

	public final String getDefaultDomainIdPrefix() {
		return "F";
	}

	public final ITypedDao<Facility> getDao() {
		return DAO;
	}

	public final Organization getParentOrganization() {
		return parentOrganization;
	}

	public final IDomainObject getParent() {
		return parentOrganization;
	}

	public final void setParent(final IDomainObject inParent) {
		if (inParent instanceof Organization) {
			parentOrganization = (Organization) inParent;
		}
	}

	public final void setParentOrganization(final Organization inOrganization) {
		parentOrganization = inOrganization;
	}

	public final String getParentOrganizationID() {
		String result = "";
		Organization organization = getParentOrganization();
		if (organization != null) {
			result = organization.getShortDomainId();
		}
		return result;
	}

	public final void setFacilityId(String inFacilityId) {
		setShortDomainId(inFacilityId);
	}

	public final void addAisle(Aisle inAisle) {
		aisles.add(inAisle);
	}

	public final void removeAisle(Aisle inAisle) {
		aisles.remove(inAisle);
	}

	public final void addContainer(Container inContainer) {
		containers.put(inContainer.getFullDomainId(), inContainer);
	}

	public final Container getContainer(String inContainerId) {
		return containers.get(normalizeChildDomainId(inContainerId));
	}

	public final void removeContainer(String inContainerId) {
		containers.remove(inContainerId);
	}

	public final void addContainerKind(ContainerKind inContainerKind) {
		containerKinds.put(inContainerKind.getFullDomainId(), inContainerKind);
	}

	public final ContainerKind getContainerKind(String inContainerKindId) {
		return containerKinds.get(normalizeChildDomainId(inContainerKindId));
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
		uomMasters.put(inUomMaster.getFullDomainId(), inUomMaster);
	}

	public final UomMaster getUomMaster(String inUomMasterId) {
		return uomMasters.get(normalizeChildDomainId(inUomMasterId));
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
			if (orderGroup.getShortDomainId().equals(inOrderGroupID)) {
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
			if (order.getShortDomainId().equals(inOrderID)) {
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
	public final void createAisle(final Double inPosXMeters,
		final Double inPosYMeters,
		final Double inProtoBayXDimMeters,
		final Double inProtoBayYDimMeters,
		final Double inProtoBayZDimMeters,
		final Integer inBaysHigh,
		final Integer inBaysLong,
		final Boolean inRunInXDir,
		final Boolean inCreateBackToBack) {
		Aisle aisle = new Aisle(this, inPosXMeters, inPosYMeters);
		try {
			Aisle.DAO.store(aisle);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		Double anchorPosX = 0.0;
		Double anchorPosY = 0.0;
		Double aisleBoundaryX = 0.0;
		Double aisleBoundaryY = 0.0;

		for (int bayLongNum = 0; bayLongNum < inBaysLong; bayLongNum++) {
			Double anchorPosZ = 0.0;
			for (int bayHighNum = 0; bayHighNum < inBaysHigh; bayHighNum++) {
				Bay protoBay = new Bay(aisle, anchorPosX, anchorPosY, anchorPosZ);
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
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inShortDomainId
	 * @param inPosTypeByStr
	 * @param inPosX
	 * @param inPosY
	 * @param inDrawOrder
	 */
	public final void createVertex(final String inShortDomainId, final String inPosTypeByStr, final Double inPosX, final Double inPosY, final Integer inDrawOrder) {

		Vertex vertex = new Vertex();
		vertex.setParentLocation(this);
		vertex.setShortDomainId(inShortDomainId);
		vertex.setPosTypeByStr(inPosTypeByStr);
		vertex.setPosX(inPosX);
		vertex.setPosY(inPosY);
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
			Vertex vertex1 = new Vertex(inLocation, PositionTypeEnum.METERS_FROM_PARENT, 0, 0.0, 0.0);
			Vertex.DAO.store(vertex1);
			Vertex vertex2 = new Vertex(inLocation, PositionTypeEnum.METERS_FROM_PARENT, 1, inXDimMeters, 0.0);
			Vertex.DAO.store(vertex2);
			Vertex vertex4 = new Vertex(inLocation, PositionTypeEnum.METERS_FROM_PARENT, 2, inXDimMeters, inYDimMeters);
			Vertex.DAO.store(vertex4);
			Vertex vertex3 = new Vertex(inLocation, PositionTypeEnum.METERS_FROM_PARENT, 3, 0.0, inYDimMeters);
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
	public final void createDefaultContainerKind() {
		ContainerKind containerKind = createContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND, 0.0, 0.0, 0.0);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final ContainerKind createContainerKind(String inShortDomainId, Double inLengthMeters, Double inWidthMeters, Double inHeightMeters) {

		ContainerKind result = null;

		result = new ContainerKind();
		result.setParentFacility(this);
		result.setShortDomainId(inShortDomainId);
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
		result.setParentFacility(this);
		result.setShortDomainId(result.computeDefaultDomainId());
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
