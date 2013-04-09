/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UomMaster.java,v 1.14 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Table(name = "uom_master", schema = "codeshelf")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class UomMaster extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<UomMaster>	DAO;

	@Singleton
	public static class UomMasterDao extends GenericDaoABC<UomMaster> implements ITypedDao<UomMaster> {
		public final Class<UomMaster> getDaoClass() {
			return UomMaster.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(UomMaster.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility			parent;

	public UomMaster() {

	}

	public final ITypedDao<UomMaster> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "UOM";
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String getUomMasterId() {
		return getDomainId();
	}

	public final void setUomMasterId(String inUomMasterId) {
		setDomainId(inUomMasterId);
	}
}
