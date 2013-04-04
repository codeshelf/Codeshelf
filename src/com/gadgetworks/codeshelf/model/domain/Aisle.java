/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.22 2013/04/04 19:05:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Aisle
 * 
 * Aisle is a facility-level location that holds a collection of bays.
 * 
 * @author jeffw
 */

@Entity
@DiscriminatorValue("AISLE")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public class Aisle extends SubLocationABC<Facility> {

	@Inject
	public static ITypedDao<Aisle>	DAO;

	@Singleton
	public static class AisleDao extends GenericDaoABC<Aisle> implements ITypedDao<Aisle> {
		public final Class<Aisle> getDaoClass() {
			return Aisle.class;
		}
	}

	private static final Logger			LOGGER	= LoggerFactory.getLogger(Aisle.class);

	public Aisle(final Facility inParentFacility, final String inAisleId, final Double inPosX, final double inPosY) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY);
		setParent(inParentFacility);
		setDomainId(inAisleId);
	}

	public final ITypedDao<Aisle> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "A";
	}

	// --------------------------------------------------------------------------
	/**
	 * Create the aisle's paths.  (Eventually we may support this on all location types.)
	 * @param inXDimMeters
	 * @param inYDimMeters
	 * @param inTravelDirection
	 */
	public final void createPaths(final Double inXDimMeters, final Double inYDimMeters, final TravelDirectionEnum inTravelDirection, boolean inOpensLowSide) {

		// Create the default path for this aisle.
		Path path = Path.DAO.findByDomainId(getParent(), Path.DEFAULT_FACILITY_PATH_ID);
		if (path == null) {
			path = new Path();
			path.setParent(getParent());
			path.setDomainId(Path.DEFAULT_FACILITY_PATH_ID);
			path.setDescription("Default Facility Path");
			path.setTravelDirEnum(inTravelDirection);
			try {
				Path.DAO.store(path);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
		path.createDefaultWorkArea();
		path.createPathSegments(this, inXDimMeters, inYDimMeters, inOpensLowSide);

		// Now loop through every location in the aisle and recompute its distance from the path.
		for (LocationABC location : getChildren()) {
			location.computePathDistance();
		}
	}
}
