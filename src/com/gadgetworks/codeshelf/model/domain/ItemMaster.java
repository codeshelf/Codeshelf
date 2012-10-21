/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemMaster.java,v 1.7 2012/10/21 02:02:17 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.LotHandlingEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "ITEMMASTER")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ItemMaster extends DomainObjectABC {

	@Inject
	public static ITypedDao<ItemMaster>	DAO;

	@Singleton
	public static class ItemMasterDao extends GenericDaoABC<ItemMaster> implements ITypedDao<ItemMaster> {
		public final Class<ItemMaster> getDaoClass() {
			return ItemMaster.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(ItemMaster.class);

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

	// The standard UoM.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private UomMaster			standardUoM;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<Item>			items	= new ArrayList<Item>();

	public ItemMaster() {

	}

	public final ITypedDao<ItemMaster> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "IM";
	}

	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	public final List<? extends IDomainObject> getChildren() {
		return getItems();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addItem(Item inItem) {
		items.add(inItem);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeItem(Item inItem) {
		items.remove(inItem);
	}

	public final String getItemMasterId() {
		return getShortDomainId();
	}

	public final void setItemMasterId(String inItemMasterId) {
		setShortDomainId(inItemMasterId);
	}
}
