/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Item.java,v 1.23 2013/04/11 22:47:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
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
@Table(name = "item", schema = "codeshelf")
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Item extends DomainObjectTreeABC<LocationABC> {

	@Inject
	public static ITypedDao<Item>	DAO;

	@Singleton
	public static class ItemDao extends GenericDaoABC<Item> implements ITypedDao<Item> {
		@Inject
		public ItemDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<Item> getDaoClass() {
			return Item.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Item.class);

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	//	@Setter
	private LocationABC			parent;

	// The item master.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private ItemMaster			itemMaster;

	// Quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				quantity;

	// The actual UoM.
	@Column(nullable = false)
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private UomMaster			uomMaster;

	// Ddc position.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private Double				ddcPosition;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			updated;

	public Item() {
	}

	public final ITypedDao<Item> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "IT";
	}

	public final String getItemId() {
		return getDomainId();
	}

	public final void setItemId(final String inItemId) {
		setDomainId(inItemId);
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final void setParent(LocationABC inParent) {
		parent = inParent;
	}
}
