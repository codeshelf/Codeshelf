/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Item.java,v 1.8 2012/10/28 01:30:57 jeffw Exp $
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
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

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
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Item extends DomainObjectABC {

	@Inject
	public static ITypedDao<Item>	DAO;

	@Singleton
	public static class ItemDao extends GenericDaoABC<Item> implements ITypedDao<Item> {
		public final Class<Item> getDaoClass() {
			return Item.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Item.class);

	// The owning item master.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private ItemMaster			parent;

	// The stored location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private LocationABC			location;

	// Quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				quantity;

	// The actual UoM.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private UomMaster			uomMaster;

	public Item() {
	}

	public final ITypedDao<Item> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "IT";
	}

	public final ItemMaster getParentItemMaster() {
		return parent;
	}

	public final String getItemsId() {
		return getShortDomainId();
	}

	public final void setItemId(final String inItemId) {
//		String uniqueItemId = location.getShortDomainId() + "." + inItemId;
		setShortDomainId(inItemId);
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

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
