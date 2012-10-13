/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UomMaster.java,v 1.5 2012/10/13 22:14:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * UomMaster
 * 
 * Hold the properties of system and user-defined units of measure.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "UOMMASTER")
@CacheStrategy
public class UomMaster extends DomainObjectABC {

	@Inject
	public static ITypedDao<UomMaster>	DAO;

	@Singleton
	public static class UomMasterDao extends GenericDaoABC<UomMaster> implements ITypedDao<UomMaster> {
		public final Class<UomMaster> getDaoClass() {
			return UomMaster.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(UomMaster.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Facility			parent;

	public UomMaster() {

	}

	@JsonIgnore
	public final ITypedDao<UomMaster> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "UOM";
	}

	@JsonIgnore
	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	@JsonIgnore
	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	@JsonIgnore
	public final String getUomMasterId() {
		return getShortDomainId();
	}

	public final void setUomMasterId(String inUomMasterId) {
		setShortDomainId(inUomMasterId);
	}
}
