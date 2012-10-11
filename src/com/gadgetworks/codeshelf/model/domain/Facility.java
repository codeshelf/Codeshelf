/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.22 2012/10/11 09:04:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Delegate;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

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
@Table(name = "LOCATION")
@DiscriminatorValue("FACILITY")
@CacheStrategy
@ToString
public class Facility extends LocationABC {

	@Inject
	public static ITypedDao<Facility>	DAO;

	@Singleton
	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}
	}

	private static final Log		LOGGER			= LogFactory.getLog(Facility.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Organization			parentOrganization;

	// These are all the uom masters for this facility.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@JsonIgnore
	@Getter
	private List<UomMaster>			uomMasters		= new ArrayList<UomMaster>();

	// These are all the item masters for this facility.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@JsonIgnore
	@Getter
	private List<ItemMaster>		itemMasters		= new ArrayList<ItemMaster>();

	// These are all the order groups for this facility.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@JsonIgnore
	@Getter
	private List<OrderGroup>		orderGroups		= new ArrayList<OrderGroup>();

	// These are all the order headers for this facility.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@JsonIgnore
	@Getter
	private List<OrderHeader>		orderHeaders	= new ArrayList<OrderHeader>();

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@JsonIgnore
	@Getter
	private List<Aisle>				aisles			= new ArrayList<Aisle>();

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@JsonIgnore
	@Getter
	private List<CodeShelfNetwork>	networks		= new ArrayList<CodeShelfNetwork>();

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, targetEntity = DropboxService.class)
	@JsonIgnore
	@Getter
	private List<IEdiService>		ediServices		= new ArrayList<IEdiService>();

	public Facility() {
		// Facilities have no parent location, but we don't want to allow ANY location to not have a parent.
		// So in this case we make the facility its own parent.  It's also a way to know when we've topped-out in the location tree.
		parent = this;
		orderHeaders = new ArrayList<OrderHeader>();
		uomMasters = new ArrayList<UomMaster>();
	}

	public Facility(final Double inPosX, final Double inPosY) {
		super(PositionTypeEnum.GPS, inPosX, inPosY);
		// Facilities have no parent location, but we don't want to allow ANY location to not have a parent.
		// So in this case we make the facility its own parent.  It's also a way to know when we've topped-out in the location tree.
		parent = this;
		orderHeaders = new ArrayList<OrderHeader>();
		uomMasters = new ArrayList<UomMaster>();
	}

	public final String getDefaultDomainIdPrefix() {
		return "F";
	}

	@JsonIgnore
	public final ITypedDao<Facility> getDao() {
		return DAO;
	}

	@JsonIgnore
	public final Organization getParentOrganization() {
		return parentOrganization;
	}

	@JsonIgnore
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
		Organization parentOrganization = getParentOrganization();
		if (parentOrganization != null) {
			result = parentOrganization.getShortDomainId();
		}
		return result;
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addAisle(Aisle inAisle) {
		aisles.add(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeAisle(Aisle inAisle) {
		aisles.remove(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addEdiService(IEdiService inEdiService) {
		ediServices.add(inEdiService);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeEdiService(IEdiService inEdiService) {
		ediServices.remove(inEdiService);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addOrderHeader(OrderHeader inOrderHeader) {
		orderHeaders.add(inOrderHeader);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeOrderHeader(OrderHeader inOrderHeaders) {
		orderHeaders.remove(inOrderHeaders);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addOrderGroup(OrderGroup inOrderGroup) {
		orderGroups.add(inOrderGroup);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeOrderGroup(OrderGroup inOrderGroups) {
		orderGroups.remove(inOrderGroups);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addItemMaster(ItemMaster inItemMaster) {
		itemMasters.add(inItemMaster);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeItemMaster(ItemMaster inItemMasters) {
		itemMasters.remove(inItemMasters);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addUomMaster(UomMaster inUomMaster) {
		uomMasters.add(inUomMaster);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeUomMaster(UomMaster inUomMasters) {
		uomMasters.remove(inUomMasters);
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
	public final void createAisle(Double inPosXMeters,
		Double inPosYMeters,
		Double inProtoBayXDimMeters,
		Double inProtoBayYDimMeters,
		Double inProtoBayZDimMeters,
		Integer inBaysHigh,
		Integer inBaysLong,
		Boolean inRunInXDir,
		Boolean inCreateBackToBack) {
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
	@JsonIgnore
	public final DropboxService getDropboxService() {
		DropboxService result = null;

		for (IEdiService ediService : getEdiServices()) {
			if (ediService instanceof DropboxService) {
				result = (DropboxService) ediService;
			}
			break;
		}

		if (result == null) {
			result = this.createDropboxService();
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
