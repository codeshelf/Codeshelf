/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UomMaster.java,v 1.18 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
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
@Table(name = "uom_master")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class UomMaster extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<UomMaster>	DAO;

	@Singleton
	public static class UomMasterDao extends GenericDaoABC<UomMaster> implements ITypedDao<UomMaster> {
		@Inject
		public UomMasterDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<UomMaster> getDaoClass() {
			return UomMaster.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(UomMaster.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
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
