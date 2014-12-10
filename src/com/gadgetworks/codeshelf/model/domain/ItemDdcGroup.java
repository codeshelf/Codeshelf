/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemDdcGroup.java,v 1.2 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * ItemDdcGroup
 * 
 * Information about a collection of items in a location that share a DDC.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "item_ddc_group")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ItemDdcGroup extends DomainObjectTreeABC<Location> {

	@Inject
	public static ITypedDao<ItemDdcGroup>	DAO;

	@Singleton
	public static class ItemDdcGroupDao extends GenericDaoABC<ItemDdcGroup> implements ITypedDao<ItemDdcGroup> {
		@Inject
		public ItemDdcGroupDao(PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<ItemDdcGroup> getDaoClass() {
			return ItemDdcGroup.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ItemDdcGroup.class);

	// The parent location.
	@ManyToOne(optional = false,fetch=FetchType.LAZY)
	private Location			parent;

	// The start position of this DDC group along the pick path.
	@Column(nullable = true,name="start_pos_along_path")
	@Getter
	@Setter
	@JsonProperty
	private Double				startPosAlongPath;

	// The end position of this DDC group along the pick path.
	@Column(nullable = true,name="end_pos_along_path")
	@Getter
	@Setter
	@JsonProperty
	private Double				endPosAlongPath;

	public ItemDdcGroup() {

	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<ItemDdcGroup> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "DDC";
	}

	public final Location getParent() {
		if (parent instanceof HibernateProxy) {
			this.parent = (Location) deproxify(this.parent);
		}		
		return parent;
	}
	
	public final void setDdcGroupId(final String inDdcGroupId) {
		setDomainId(inDdcGroupId);
	}
	
	public final String getDdcGroupId() {
		return getDomainId();
	}

	public final void setParent(Location inParent) {
		parent = inParent;
	}

	public final Facility getFacility() {
		return getParent().getFacility();
	}

}
