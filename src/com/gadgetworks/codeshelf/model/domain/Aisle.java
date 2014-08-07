/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.26 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public class Aisle extends SubLocationABC<Facility> {

	@Inject
	public static ITypedDao<Aisle>	DAO;

	@Singleton
	public static class AisleDao extends GenericDaoABC<Aisle> implements ITypedDao<Aisle> {
		@Inject
		public AisleDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<Aisle> getDaoClass() {
			return Aisle.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Aisle.class);

	public Aisle(final Facility inParentFacility, final String inAisleId, final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(inAnchorPoint, inPickFaceEndPoint);
		setParent(inParentFacility);
		setDomainId(inAisleId);
		inParentFacility.addAisle(this);
	}

	public final ITypedDao<Aisle> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "A";
	}

	public final void associatePathSegment(String inPathSegPersistentID) {
		// to support setting of list view meta-field pathSegId

		// Get the PathSegment
		UUID persistentId = UUID.fromString(inPathSegPersistentID);
		PathSegment pathSegment = PathSegment.DAO.findByPersistentId(persistentId);

		if (pathSegment != null) {
			// Some checking 
			int initialLocationCount = pathSegment.getLocations().size();

			this.setPathSegment(pathSegment);
			ITypedDao<Aisle> theDao = this.getDao();
			theDao.store(this);

			// There is now a new association. Need to recompute locations positions along the path.  Kind of too bad to do several times as each segment is assigned.
			// Note, this is also done on application restart.
			Facility theFacility = this.getParent();
			Path thePath = pathSegment.getParent();
			if (thePath == null || theFacility == null)
				LOGGER.error("null value in associatePathSegment");
			else {
				theFacility.recomputeLocationPathDistances(thePath);
			}

			int afterLocationCount = pathSegment.getLocations().size();
			if (initialLocationCount == afterLocationCount)
				LOGGER.error("associatePathSegment did not correctly update locations array");

		} else {
			throw new DaoException("Could not associate path segment, segment not found: " + inPathSegPersistentID);
		}
	}

	public final void setControllerChannel(String inControllerPersistentIDStr, String inChannelStr) {
		doSetControllerChannel(inControllerPersistentIDStr, inChannelStr);
	}

	public final Boolean doesAisleIncreaseForwardAlongPath() {
		// JR in progress for DEV-310
		// The answer depends on the aisle's relationship to its pathSegment
		Boolean returnValue = true;
		
		PathSegment mySegment = getPathSegment();
		if ( false  && mySegment != null) {
			// are we X oriented or Y oriented. Could get that from either the aisle or the path. Here we ask the aisle
			Boolean xOriented = this.getPickFaceEndPosY() == 0.0;
			
			if (xOriented) {
				// Think about standing on the path, and looking at the aisle pickface. By our convention, B1 and and S1 are left as we look. But is further along the path or not?
				// That depends on whether the path is above (Y coordinate) the aisle or not.
				Double aisleY = this.getAnchorPosY();
				Double pathY = mySegment.getStartPosY(); // assume start and end Y are roughly the same.
				Boolean pathSegFlowsRight =  mySegment.getEndPosX() > mySegment.getStartPosX();
				
				// if aisle above the path, and path going right, then B1, B2, etc. will increase along the path
				returnValue = ((aisleY < pathY) == pathSegFlowsRight);
			}
			else {
				Double aisleX = this.getAnchorPosX();
				Double pathX = mySegment.getStartPosX(); // assume start and end X are roughly the same.
				Boolean pathSegFlowsDown =  mySegment.getEndPosY() > mySegment.getStartPosY();
				returnValue = ((aisleX < pathX) == pathSegFlowsDown);

			}
		}
		
		// return returnValue;
		return true;
	}

}
