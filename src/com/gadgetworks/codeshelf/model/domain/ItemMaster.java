/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemMaster.java,v 1.21 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.LotHandlingEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * ItemMaster
 * 
 * The invariant properties of items used in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "item_master", schema = "codeshelf")
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ItemMaster extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<ItemMaster>	DAO;

	@Singleton
	public static class ItemMasterDao extends GenericDaoABC<ItemMaster> implements ITypedDao<ItemMaster> {
		@Inject
		public ItemMasterDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<ItemMaster> getDaoClass() {
			return ItemMaster.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ItemMaster.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility			parent;

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String				description;

	// The lot handling method for this item.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private LotHandlingEnum		lotHandlingEnum;

	// Ddc Id
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String				ddcId;

	// Ddc pack depth
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Integer				ddcPackDepth;

	// The standard UoM.
	@Column(nullable = false)
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private UomMaster			standardUom;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "itemMaster")
	@Getter
	private List<Item>			items	= new ArrayList<Item>();

	public ItemMaster() {
		lotHandlingEnum = LotHandlingEnum.FIFO;
	}

	public final ITypedDao<ItemMaster> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "IM";
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return getItems();
	}
	
	public final void setItemId(final String inItemId) {
		setDomainId(inItemId);
	}
	
	public final String getItemId() {
		return getDomainId();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addItem(Item inItem) {
		items.add(inItem);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeItem(Item inItem) {
		items.remove(inItem);
	}

	public Boolean isDdcItem() {
		return (ddcId != null);
	}
}
