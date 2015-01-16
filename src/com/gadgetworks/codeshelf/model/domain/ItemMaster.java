/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemMaster.java,v 1.29 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.LotHandlingEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.PropertyService;
import com.gadgetworks.codeshelf.util.ASCIIAlphanumericComparator;
import com.gadgetworks.codeshelf.util.UomNormalizer;
import com.google.common.base.Joiner;
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
@Table(name = "item_master")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ItemMaster extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<ItemMaster>	DAO;

	@Singleton
	public static class ItemMasterDao extends GenericDaoABC<ItemMaster> implements ITypedDao<ItemMaster> {
		@Inject
		public ItemMasterDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<ItemMaster> getDaoClass() {
			return ItemMaster.class;
		}
	}

	private static final Logger				LOGGER						= LoggerFactory.getLogger(ItemMaster.class);

	private static final Comparator<String>	asciiAlphanumericComparator	= new ASCIIAlphanumericComparator();

	// The parent facility.
	@ManyToOne(optional = false,fetch=FetchType.LAZY)
	private Facility						parent;

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String							description;

	// The lot handling method for this item.
	@Column(nullable = false,name="lot_handling")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private LotHandlingEnum					lotHandlingEnum;

	// Ddc Id
	@Column(nullable = true,name="ddc_id")
	@Getter
	@Setter
	@JsonProperty
	private String							ddcId;

	// SlotFlex Id
	@Column(nullable = true,name="slot_flex_id")
	@Getter
	@Setter
	@JsonProperty
	private String							slotFlexId;

	// Ddc pack depth
	@Column(nullable = true,name="ddc_pack_depth")
	@Getter
	@Setter
	@JsonProperty
	private Integer							ddcPackDepth;

	// The standard UoM.
	@ManyToOne(optional = false,fetch=FetchType.LAZY)
	@JoinColumn(name="standard_uom_persistentid")
	@Setter
	private UomMaster						standardUom;

	// For a network this is a list of all of the users that belong in the set.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean							active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp						updated;

	@OneToMany(mappedBy = "parent")
	@Getter
	private List<Item>						items						= new ArrayList<Item>();

	public ItemMaster() {
		lotHandlingEnum = LotHandlingEnum.FIFO;
		active = true;
		updated = new Timestamp(System.currentTimeMillis());
	}

	public UomMaster getStandardUom() {
		if (standardUom instanceof HibernateProxy) {
			this.standardUom = (UomMaster) PersistenceService.deproxify(this.standardUom);
		}
		return standardUom;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<ItemMaster> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "IM";
	}

	public final Facility getParent() {
		if (parent instanceof HibernateProxy) {
			this.parent = (Facility) PersistenceService.deproxify(this.parent);
		}		
		return parent;
	}

	public final Facility getFacility() {
		return getParent();
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return getItems();
	}

	/**
	 * Item's domainId changes as the item moves. Don't use hashmap by domainId.
	 */
	public final void addItemToMaster(Item inItem) {
		ItemMaster previousItemMaster = inItem.getParent();
		if (previousItemMaster == null) {
			items.add(inItem);
			inItem.setParent(this);
		} else if (!previousItemMaster.equals(this)) {
			LOGGER.error("cannot add Item " + inItem.getDomainId() + " to " + this.getDomainId()
					+ ". because item should not change parent from " + previousItemMaster.getDomainId());
		} else {
			LOGGER.error("Extra unnecessary call to addItemToMaster");
		}
	}

	/**
	 * Careful with removeItem(). Item's domainId changes as the item moves.
	 */
	public final void removeItemFromMaster(Item inItem) {
		if (items.contains(inItem)) {
			inItem.setParent(null);
			items.remove(inItem);
		} else {
			LOGGER.error("cannot remove Item " + inItem.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void setItemId(final String inItemId) {
		setDomainId(inItemId);
	}

	public final String getItemId() {
		return getDomainId();
	}

	public Boolean isDdcItem() {
		return (ddcId != null);
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the first item with matching uom along the path (in path working order).
	 * This is used for cart setup for outbound orders (classic pick)
	 * No isActive() seen in the code here. isLocationOnPath() checks that
	 * @param inPath
	 * @param inUomStr
	 * @return
	 */
	public final Item getFirstActiveItemMatchingUomOnPath(final Path inPath, final String inUomStr) {
		Item result = null;

		String normalizedUomStr = UomNormalizer.normalizeString(inUomStr);

		Location foundLocation = null;
		Item selectedItem = null;

		// This mimics the old code. Not at all sure it is correct.
		for (Item item : getItems()) {
			// Does the Item know where it is?
			Location location = (Location) item.getStoredLocation();

			if (location != null && inPath.isLocationOnPath(location)) {
				String itemUom = item.getUomMasterId();
				String itemNormalizedUom = UomNormalizer.normalizeString(itemUom);
				if (normalizedUomStr.equals(itemNormalizedUom)) {
					foundLocation = location;
					selectedItem = item;
					break;
				}
			}
		}

		// The item is on the CHE's path, so add it.
		if (foundLocation != null) {
			result = selectedItem;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * For location-based pick
	 * This is used for cart setup for outbound orders (classic pick)
	 * Return null if location does not match. Also null for uom mismatch or not on path.
	*/
	public final Item getActiveItemMatchingLocUomOnPath(final Location inLocation, final Path inPath, final String inUomStr) {
		Item result = null;

		String normalizedUomStr = UomNormalizer.normalizeString(inUomStr);

		Location foundLocation = null;
		Item selectedItem = null;

		for (Item item : getItems()) {
			Location location = (Location) item.getStoredLocation();
			if (location ==  null || !location.equals(inLocation))
				continue;

			// Perhaps we should logger.warn if we found matching uom item, but it is not on our path. Not doing now.
			if (inPath.isLocationOnPath(location)) {
				String itemUom = item.getUomMasterId();
				String itemNormalizedUom = UomNormalizer.normalizeString(itemUom);
				if (normalizedUomStr.equals(itemNormalizedUom)) {
					foundLocation = location;
					selectedItem = item;
					break;
				}
			}
		}

		// The item is on the CHE's path, so add it.
		if (foundLocation != null) {
			result = selectedItem;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return list of inventory items of the right UOM for that SKU
	 * @param inUomStr
	 * @return
	 */
	public final List<Item> getItemsOfUom(final String inUomStr) {
		String normalizedUomStr = UomNormalizer.normalizeString(inUomStr);
		List<Item> theReturnList = new ArrayList<Item>();
		for (Item item : getItems()) {
			String itemUom = item.getUomMasterId();
			String itemNormalizedUom = UomNormalizer.normalizeString(itemUom);
			if (normalizedUomStr.equals(itemNormalizedUom))
				theReturnList.add(item);
		}
		return theReturnList;
	}

	// --------------------------------------------------------------------------
	// metafields
	public final String getItemLocations() {
		List<String> itemLocationIds = new ArrayList<String>();
		List<Item> items = getItems();
		//filter by uom and join the aliases together
		for (Item item : items) {
			String itemLocationId = item.getStoredLocation().getPrimaryAliasId();
			itemLocationIds.add(itemLocationId);
		}
		Collections.sort(itemLocationIds, asciiAlphanumericComparator);
		return Joiner.on(",").join(itemLocationIds);
	}

	public static void setDao(ItemMasterDao inItemMasterDao) {
		ItemMaster.DAO = inItemMasterDao;
	}

	/*
	 * Assuming the existing item would be on its master:
	 * 1) Find the existing. 2) Disallow same location, master, uom combination. 3) Disallow same master, each anywhere.
	 * (Disallow means to return an existing item, even if some details mismatch.)
	 * Will need to modify for lots.
	 */
	private Item findExistingItem(Location inLocation, UomMaster inUom) {
		String thisUomId = inUom.getUomMasterId();
		boolean thisItemEach = UomNormalizer.isEach(thisUomId);
		Facility facility = inLocation.getFacility();
		boolean eachMult =  PropertyService.getBooleanPropertyFromConfig(facility,DomainObjectProperty.EACHMULT);
		if (thisItemEach && !eachMult) {
			for (Item item : getItems()) {
				if (UomNormalizer.isEach(item.getUomMasterId())) return item;
			}
		} 
		else {
			String domainId = Item.makeDomainId(this.getItemId(), inLocation, thisUomId);
			for (Item item : getItems()) {
				// if items follow the normal pattern, equals on domainId would be sufficient				
				if (domainId.equals(item.getDomainId()))
					return item;
				else if (inLocation.equals(item.getStoredLocation())) {
					if (UomNormalizer.normalizedEquals(thisUomId, item.getUomMasterId())) {
						LOGGER.error("findExistingItem succeeded with domainId mismatch");
						return item;
					}
				}
			}
		}
		return null;
	}

	/*
	 * This creates a new Item for this master, and adds to the location. All business logic checks need to have been done before:	 * 1) Find the existing. 2) Disallow same location, master, uom combination. 3) Disallow same master, each anywhere.
	 */
	private Item createStoredItem(Location location, UomMaster uom) {
		String domainId = Item.makeDomainId(this.getItemId(), location, uom.getUomMasterId());
		Item item = new Item();
		item.setDomainId(domainId);

		this.addItemToMaster(item);

		item.setUomMaster(uom);
		location.addStoredItem(item);

		return item;
	}

	/*
	 * Huge side effect for each items. If found, and location is different, update the location. Caller must persist the change.
	 */
	public final Item findOrCreateItem(Location inLocation, UomMaster uom) {
		Item item = findExistingItem(inLocation, uom);
		if (item == null)
			item = createStoredItem(inLocation, uom);
		else if (!item.getStoredLocation().equals(inLocation))
			inLocation.addStoredItem(item); // which removes from the prior location.
		return item;
	}

}
