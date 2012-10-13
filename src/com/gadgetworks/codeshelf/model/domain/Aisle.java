/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.10 2012/10/13 22:14:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
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
@CacheStrategy
public class Aisle extends LocationABC {

	@Inject
	public static ITypedDao<Aisle>	DAO;

	@Singleton
	public static class AisleDao extends GenericDaoABC<Aisle> implements ITypedDao<Aisle> {
		public final Class<Aisle> getDaoClass() {
			return Aisle.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Aisle.class);

	public Aisle(final Facility inParentFacility, final Double inPosX, final double inPosY) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY);
		setParent(inParentFacility);
		setShortDomainId(computeDefaultDomainId());
	}

	@JsonIgnore
	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(final IDomainObject inParent) {
		if (inParent instanceof Facility) {
			parent = (Facility) inParent;
		}
	}

	@JsonIgnore
	public final ITypedDao<Aisle> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "A";
	}
}
