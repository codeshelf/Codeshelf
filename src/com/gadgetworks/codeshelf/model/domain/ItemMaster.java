/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemMaster.java,v 1.29 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.InventorySlottedCsvBean;
import com.gadgetworks.codeshelf.model.LotHandlingEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.util.UomNormalizer;
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
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
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

	// SlotFlex Id
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String				slotFlexId;

	// Ddc pack depth
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Integer				ddcPackDepth;

	// The standard UoM.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private UomMaster			standardUom;

	// For a network this is a list of all of the users that belong in the set.
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

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, Item>	items		= new HashMap<String, Item>();

	public ItemMaster() {
		lotHandlingEnum = LotHandlingEnum.FIFO;
	}
	
	public ItemMaster(Facility inParent, String inItemId, UomMaster standardUom) {
		super(inItemId);
		this.parent = inParent;
		this.standardUom = standardUom;
		lotHandlingEnum = LotHandlingEnum.FIFO;
		active = true;
		updated = new Timestamp(System.currentTimeMillis());
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
	
	public final void addItem(Item inItem) {
		items.put(inItem.getDomainId(), inItem);
	}

	public final Item getItem(String inItemId) {
		return items.get(inItemId);
	}

	public final void removeNetwork(String inNetworkId) {
		items.remove(inNetworkId);
	}

	public final List<Item> getItems() {
		return new ArrayList<Item>(items.values());
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
	 * Return the first item with position along the path (in path working order).
	 * This is used for cart setup for outbound orders (classic pick)
	 * @param inPath
	 * @return
	 */
	public final Item getFirstItemOnPath(final Path inPath) {
		Item result = null;

		ISubLocation foundLocation = null;
		Item selectedItem = null;
		
		// This mimics the old code. Not at all sure it is correct.
		for (Item item : getItems()) {
			// Does the Item know where it is?
			ISubLocation location = (ISubLocation) item.getStoredLocation();
			
			if (location != null && inPath.isLocationOnPath(location)) {
				foundLocation = location;
				selectedItem = item;
				break;
			}
		}

		// The item is on the CHE's path, so add it.
		if (foundLocation != null) {
			result = selectedItem;
		}
		
		// What might we do better?
		// See getFirstOrderLocationOnPath(). It checks the distance along the path.
		// We definitely could check the uom to make sure the item uom matches the order uom. That we, we don't send case pick to each pick area, and vice versa.
		// If we believe in our inventory, we might check the item quantity to see if there is enough for the order detail quantity. But we don't even pass that in.

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the first item with matching uom along the path (in path working order).
	 * This is used for cart setup for outbound orders (classic pick)
	 * @param inPath
	 * @param inUomStr
	 * @return
	 */
	public final Item getFirstItemMatchingUomOnPath(final Path inPath, final String inUomStr) {
		Item result = null;
		
		String normalizedUomStr = UomNormalizer.normalizeString(inUomStr);

		ISubLocation foundLocation = null;
		Item selectedItem = null;
		
		// This mimics the old code. Not at all sure it is correct.
		for (Item item : getItems()) {
			// Does the Item know where it is?
			ISubLocation location = (ISubLocation) item.getStoredLocation();
			
			if (location != null && inPath.isLocationOnPath(location)) {
				String itemUom = item.getUomMasterId();
				String itemNormalizedUom = UomNormalizer.normalizeString(itemUom);
				if (normalizedUomStr.equals(itemNormalizedUom)){
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
		List<Item> theReturnList = new ArrayList<Item>() ;
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
		return "";
	}



}
