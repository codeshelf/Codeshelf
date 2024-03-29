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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.LotHandlingEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.ASCIIAlphanumericComparator;
import com.codeshelf.util.UomNormalizer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;

// --------------------------------------------------------------------------
/**
 * ItemMaster
 * 
 * The invariant properties of items used in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "item_master", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ItemMaster extends DomainObjectTreeABC<Facility> {

	public static class ItemMasterDao extends GenericDaoABC<ItemMaster> implements ITypedDao<ItemMaster> {
		public final Class<ItemMaster> getDaoClass() {
			return ItemMaster.class;
		}
	}

	private static final Logger				LOGGER						= LoggerFactory.getLogger(ItemMaster.class);

	private static final Comparator<String>	asciiAlphanumericComparator	= new ASCIIAlphanumericComparator();

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String							description;

	// The lot handling method for this item.
	@Column(nullable = false, name = "lot_handling")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private LotHandlingEnum					lotHandlingEnum;

	// Ddc Id
	@Column(nullable = true, name = "ddc_id")
	@Getter
	@Setter
	@JsonProperty
	private String							ddcId;

	// SlotFlex Id
	@Column(nullable = true, name = "slot_flex_id")
	@Getter
	@Setter
	@JsonProperty
	private String							slotFlexId;

	// Ddc pack depth
	@Column(nullable = true, name = "ddc_pack_depth")
	@Getter
	@Setter
	@JsonProperty
	private Integer							ddcPackDepth;

	// The standard UoM.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "standard_uom_persistentid")
	@Getter
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

	@OneToMany(mappedBy = "parent", orphanRemoval=true)
	@Getter
	private List<Item>						items						= new ArrayList<Item>();
	
	@OneToMany(mappedBy = "parent", orphanRemoval=true)
	@MapKey(name = "domainId")
	@Getter
	private Map<String, Gtin>				gtins						= new HashMap<String, Gtin>();
	
	public ItemMaster() {
		lotHandlingEnum = LotHandlingEnum.FIFO;
		active = true;
		updated = new Timestamp(System.currentTimeMillis());
	}

	public ItemMaster(Facility facility, String domainId, UomMaster inUom) {
		this();
		setParent(facility);
		setItemId(domainId);
		setStandardUom(inUom);
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<ItemMaster> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<ItemMaster> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ItemMaster.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "IM";
	}

	public Facility getFacility() {
		return getParent();
	}

	public List<? extends IDomainObject> getChildren() {
		return getItems();
	}

	/**
	 * Item's domainId changes as the item moves. Don't use hashmap by domainId.
	 */
	public void addItemToMaster(Item inItem) {
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
	public void removeItemFromMaster(Item inItem) {
		if (items.contains(inItem)) {
			inItem.setParent(null);
			items.remove(inItem);
		} else {
			LOGGER.error("cannot remove Item " + inItem.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}
	
	/**
	 * 
	 */
	public void addGtinToMaster(Gtin inGtinMap)	{
		ItemMaster previousItemMaster = inGtinMap.getParent();
		
		if(previousItemMaster == null)	{
			gtins.put(inGtinMap.getDomainId(), inGtinMap);
			inGtinMap.setParent(this);
		} else if(!previousItemMaster.equals(this)) {
			LOGGER.error("cannot add GtinMap " + inGtinMap.getDomainId() + " to " + this.getDomainId()
				+ ". because GtinMap should not change parent from " + previousItemMaster.getDomainId());
		} else {
			LOGGER.error("Extra unnecessary call to addGtinMapToMaster");
		}
	}
	
	public void removeGtinFromMaster(Gtin inGtinMap) {
		if (gtins.containsKey(inGtinMap.getDomainId())) {
			inGtinMap.setParent(null);
			gtins.remove(inGtinMap.getDomainId());
		} else {
			LOGGER.error("cannot remove GtinMap " + inGtinMap.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public void setItemId(final String inItemId) {
		setDomainId(inItemId);
	}

	public String getItemId() {
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
	public Item getFirstActiveItemMatchingUomOnPath(final Path inPath, final String inUomStr) {
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
	public Item getActiveItemMatchingLocUomOnPath(final Location inLocation, final Path inPath, final String inUomStr) {
		Item result = null;

		String normalizedUomStr = UomNormalizer.normalizeString(inUomStr);

		Location foundLocation = null;
		Item selectedItem = null;

		for (Item item : getItems()) {
			Location location = (Location) item.getStoredLocation();
			if (location == null || !location.equals(inLocation))
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
	 * For location-based pick
	 * This is used in EDI evaluation
	*/
	public Item getActiveItemMatchingLocUom(final Location inLocation, final UomMaster inUomMaster) {

		Item selectedItem = null;
		if (inUomMaster == null || inLocation == null) {
			LOGGER.error("bad call to getItemMatchingLocUom");
			return null;
		}

		for (Item item : getItems()) {
			if (!item.getActive())
				continue;
			Location location = (Location) item.getStoredLocation();
			if (location == null || !location.equals(inLocation))
				continue;
			UomMaster thisUomMaster = item.getUomMaster();
			if (thisUomMaster.equals(inUomMaster)) {
				selectedItem = item;
				break;
			}			
		}

		return selectedItem;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return list of inventory items of the right UOM for that SKU
	 * @param inUomStr
	 * @return
	 */
	public List<Item> getItemsOfUom(final String inUomStr) {
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
	public String getItemLocations() {
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
	

	public String getItemGtins() {
		
		List<String> gtinValues = new ArrayList<String>();
		Map<String, Gtin> gtins = getGtins();

		for (Gtin gtin : gtins.values()) {
			String gtinValue= gtin.getDomainId();
			gtinValues.add(gtinValue);
		}
		
		Collections.sort(gtinValues, asciiAlphanumericComparator);
		return Joiner.on(",").join(gtinValues);
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
		boolean eachMult = PropertyBehavior.getPropertyAsBoolean(facility, FacilityPropertyType.EACHMULT);
		boolean isLocationSkuWall = inLocation.isSkuWallLocation();
		for (Item item : getItems()) {
			boolean isItemInSkuWall = item.getStoredLocation().isSkuWallLocation();
			//Even when disallowing multiple identical Items in the facility, we still allow the same Gtin to be in a normal area and on the Sku wall
			boolean itemAndLocationAreBothInSkuWallOrOut = isLocationSkuWall == isItemInSkuWall;
			if (thisItemEach && !eachMult) {
				if (UomNormalizer.isEach(item.getUomMasterId()) && itemAndLocationAreBothInSkuWallOrOut)
					return item;
			} else {
				// if items follow the normal pattern, equals on domainId would be sufficient
				String domainId = Item.makeDomainId(this.getItemId(), inLocation, thisUomId); 
				if (domainId.equals(item.getDomainId()) && itemAndLocationAreBothInSkuWallOrOut)
					return item;
				else if (inLocation.equals(item.getStoredLocation())) {
					if (UomNormalizer.normalizedEquals(thisUomId, item.getUomMasterId()) && itemAndLocationAreBothInSkuWallOrOut) {
						LOGGER.error("findExistingItem succeeded with domainId mismatch");
						return item;
					}
				}				
			}
		}
		//Make sure that there are no identical items in the same Sku wall
		if (inLocation.isSkuWallLocation()){
			Location wall = inLocation.getWall(Location.SKUWALL_USAGE);
			Item itemInWall = wall.findItemInLocationAndChildren(this, inUom);
			if (itemInWall != null){
				return itemInWall;
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
	public Item findOrCreateItem(Location inLocation, UomMaster uom) {
		Item item = findExistingItem(inLocation, uom);
		//If no item with matching master/uom found, create new item
		if (item == null){
			item = createStoredItem(inLocation, uom);
			return item;
		}
		if (!item.getStoredLocation().equals(inLocation))
			inLocation.addStoredItem(item); // which removes from the prior location.
		return item;
	}
	
	private Gtin findExistingGtin(String inGtin) {
		return gtins.get(inGtin);
	}
	
	public Gtin findOrCreateGtin(String inGtin, UomMaster inUomMaster) {
		Gtin gtin = findExistingGtin(inGtin);
		
		if (gtin == null){
			gtin = createGtin(inGtin, inUomMaster);
		}
		
		return gtin;
	}
	
	public Gtin createGtin(String inGtin, UomMaster inUomMaster) {
		Gtin gtin = new Gtin();
		gtin.setDomainId(inGtin);
		this.addGtinToMaster(gtin);
		gtin.setUomMaster(inUomMaster);
		return gtin;
	}

	public Gtin getGtin(String gtin) {
						
		if (gtins.containsKey(gtin)){
			return gtins.get(gtin);
		} else {
			return null;
		}
	}
	
	/*
	 * Assuming only one GTIN per ItemMaster <--> UOM matching
	 * If more than one we might return the wrong GTIN, however, having multiple
	 * GTINs with the same UOM is wrong.
	 * 
	 * New from v18/v19 find a gtin for the master with the equivalent normalized uom.
	 * If both exist (shouldn't), return the exact match over the normalized match
	 * 
	 * FIXME Database restraint for GTIN ItemMaster <--> UOM matching
	 */
	public Gtin getGtinForUom(UomMaster inUomMaster){
		
		List<Gtin> gtinList = new ArrayList<Gtin>(gtins.values());
		Gtin normalizedMatch = null;
		
		for (Gtin gtin : gtinList) {
			if (gtin.getUomMaster().equals(inUomMaster))
				return gtin;
			else if (gtin.getUomMaster().equalsNormalized(inUomMaster)){
				normalizedMatch = gtin;
			}
		}		
		return normalizedMatch;
	}

}
