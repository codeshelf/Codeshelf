/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.7 2012/09/17 04:20:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "LOCATION")
@DiscriminatorValue("AISLE")
public class Aisle extends LocationABC {

	@Inject
	public static AisleDao DAO;

	@Singleton
	public static class AisleDao extends GenericDaoABC<Aisle>  implements ITypedDao<Aisle> {
		public final Class<Aisle> getDaoClass() {
			return Aisle.class;
		}
	}
	
	private static final Log	LOGGER	= LogFactory.getLog(Aisle.class);
	
	public Aisle(final Facility inParentFacility, final Double inPosX, final double inPosY) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY);
		setParentFacility(inParentFacility);
		setDomainId(computeDefaultDomainId());
	}
	
	@JsonIgnore
	public final AisleDao getDao() {
		return DAO;
	}
	
	public final String getDefaultDomainIdPrefix() {
		return "A";
	}
	
	public final IDomainObject getParent() {
		return getParentLocation();
	}
	
	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentLocation((Facility) inParent);
		}
	}
	
	public final Facility getParentFacility() {
		return (Facility) getParentLocation();
	}
	
	public final void setParentFacility(Facility inParentFacility) {
		setParentLocation(inParentFacility);
	}
}
