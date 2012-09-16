/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.10 2012/09/16 07:22:15 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

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
public class Facility extends LocationABC {

	@Inject
	public static ITypedDao<Facility>	DAO;

	@Singleton
	public static class FacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}
	}

	private static final Log		LOGGER		= LogFactory.getLog(Facility.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Organization			parentOrganization;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent")
	@JsonIgnore
	@Getter
	private List<Aisle>				aisles		= new ArrayList<Aisle>();

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent")
	@JsonIgnore
	@Getter
	private List<CodeShelfNetwork>	networks	= new ArrayList<CodeShelfNetwork>();

	public Facility() {
		// Facilities have no parent location, but we don't want to allow ANY location to not have a parent.
		// So in this case we make the facility its own parent.  It's also a way to know when we've topped-out in the location tree.
		this.setParentLocation(this);
	}

	public Facility(final Double inPosX, final double inPosY) {
		super(PositionTypeEnum.GPS, inPosX, inPosY);
		// Facilities have no parent location, but we don't want to allow ANY location to not have a parent.
		// So in this case we make the facility its own parent.  It's also a way to know when we've topped-out in the location tree.
		this.setParentLocation(this);
	}

	public final String getDefaultDomainIdPrefix() {
		return "F";
	}

	@JsonIgnore
	public final ITypedDao<Facility> getDao() {
		return DAO;
	}

	public final Organization getParentOrganization() {
		return parentOrganization;
	}

	public final void setParentOrganization(final Organization inOrganization) {
		parentOrganization = inOrganization;
	}

	public final IDomainObject getParent() {
		return getParentOrganization();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Organization) {
			setParentOrganization((Organization) inParent);
		}
	}

	public final String getParentOrganizationID() {
		return getParentOrganization().getDomainId();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addAisle(Aisle inAisle) {
		aisles.add(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeAisle(Aisle inAisle) {
		aisles.remove(inAisle);
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
}
