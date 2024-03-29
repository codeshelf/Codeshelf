/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UomMaster.java,v 1.18 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.UomNormalizer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

// --------------------------------------------------------------------------
/**
 * UomMaster
 * 
 * Hold the properties of system and user-defined units of measure.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "uom_master", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class UomMaster extends DomainObjectTreeABC<Facility> {

	public static class UomMasterDao extends GenericDaoABC<UomMaster> implements ITypedDao<UomMaster> {
		public final Class<UomMaster> getDaoClass() {
			return UomMaster.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(UomMaster.class);

	public UomMaster() {

	}

	public UomMaster(String inUomId) {
		super(inUomId);
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<UomMaster> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<UomMaster> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(UomMaster.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "UOM";
	}

	public Facility getFacility() {
		return getParent();
	}

	public String getUomMasterId() {
		return getDomainId();
	}

	public void setUomMasterId(String inUomMasterId) {
		setDomainId(inUomMasterId);
	}

	/**
	 * If there happen to be two versions of the "each" uom, then this function returns true whereas simple equals will fail. That would confuse GTIN and inventory logic.
	 */
	public boolean equalsNormalized(UomMaster inMaster) {
		if (this.equals(inMaster))
			return true;
		else if (UomNormalizer.normalizedEquals(this.getUomMasterId(), inMaster.getUomMasterId()))
			return true;
		return false;

	}

}
