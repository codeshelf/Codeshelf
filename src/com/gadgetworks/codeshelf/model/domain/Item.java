/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Item.java,v 1.1 2012/10/01 01:35:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
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
 * Item
 * 
 * An instance of an item in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "ITEM")
@CacheStrategy
public class Item extends DomainObjectABC {

	@Inject
	public static ItemDao	DAO;

	@Singleton
	public static class ItemDao extends GenericDaoABC<Item> implements ITypedDao<Item> {
		public final Class<Item> getDaoClass() {
			return Item.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Item.class);

	// Quantity.
	@Getter
	@Setter
	@Column(nullable = false)
	private Float				quantity;

	// The owning item master.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private ItemMaster			parent;

	// The actual UoM.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private UomMaster			uom;

	public Item() {
	}

	@JsonIgnore
	public final ITypedDao<Item> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "PS";
	}

	public final ItemMaster getParentItemMaster() {
		return parent;
	}

	public final void setParentItemMaster(final ItemMaster inItemMaster) {
		parent = inItemMaster;
	}

	public final IDomainObject getParent() {
		return getParentItemMaster();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof ItemMaster) {
			setParentItemMaster((ItemMaster) inParent);
		}
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
