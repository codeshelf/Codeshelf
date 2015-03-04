/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemMaster.java,v 1.29 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
@Table(name = "gtin")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Gtin extends DomainObjectTreeABC<ItemMaster> {

	@Inject
	public static ITypedDao<Gtin>	DAO;
	
	@Singleton
	public static class GtinMapDao extends GenericDaoABC<Gtin> implements ITypedDao<Gtin> {
		public final Class<Gtin> getDaoClass() {
			return Gtin.class;
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
	
	public Gtin() { }
	
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
	public final ITypedDao<Gtin> getDao() {
		return DAO;
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}
		
}
