/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemMaster.java,v 1.29 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.LotHandlingEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.service.PropertyService;
import com.codeshelf.util.ASCIIAlphanumericComparator;
import com.codeshelf.util.UomNormalizer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * GtinMap
 * 
 * A map between SKU and GTIN
 * 
 * @author huffa
 */

@Entity
@Table(name = "gtin_map")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class GtinMap extends DomainObjectTreeABC<ItemMaster> {

	@Inject
	public static ITypedDao<GtinMap>	DAO;
	
	@Singleton
	public static class GtinMapDao extends GenericDaoABC<GtinMap> implements ITypedDao<GtinMap> {
		public final Class<GtinMap> getDaoClass() {
			return GtinMap.class;
		}
	}
	
	// The owning location.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private ItemMaster			parent;

	// The actual UoM.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "uom_master_persistentid")
	@Getter
	@Setter
	private UomMaster			uomMaster;
	
	public GtinMap() { }
	
	public void setGtin(final String inGtin) {
		setDomainId(inGtin);
	}

	public String getGtin() {
		return getDomainId();
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "GTIN";
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<GtinMap> getDao() {
		return DAO;
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}
		
}
