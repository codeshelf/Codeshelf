/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Item.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
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
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistencyService;
import com.gadgetworks.codeshelf.util.StringUIConverter;
import com.gadgetworks.codeshelf.util.UomNormalizer;
import com.google.common.base.Strings;
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
@Table(name = "item")
//@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString
public class Item extends DomainObjectTreeABC<ItemMaster> {

	@Inject
	public static ITypedDao<Item>	DAO;

	@Singleton
	public static class ItemDao extends GenericDaoABC<Item> implements ITypedDao<Item> {
		@Inject
		public ItemDao(PersistencyService persistencyService) {
			super(persistencyService);
		}

		public final Class<Item> getDaoClass() {
			return Item.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Item.class);

	// The owning location.
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private ItemMaster			parent;

	// The stored location.
	@ManyToOne(optional = false)
	@Getter
	//	@Setter
	private LocationABC			storedLocation;

	// Quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				quantity;

	// The actual UoM.
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private UomMaster			uomMaster;

	// Ddc position.
	@Column(nullable = true)
	@Getter
	@Setter
	private Double				metersFromAnchor;								// used to be posAlongPath. Upgrade action doUpgrade017()

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

	// --------------------------------------------------------------------------
	/**
	 * This creates a standard domainId that keeps all of the items in different locations unique among a single ItemMaster.
	 * @param inItemId
	 * @param inLocationId
	 * @return
	 */
	public static String makeDomainId(final String inItemMasterId, final ILocation<?> inLocation, final String inUom) {
		// as soon as we have "lot" field on item, we want to either pass the lot in, or get from the item.
		// an item is defined unique combination of item master, lot, and location.
		String revisedUom = inUom;
		// Let's not be confused if some items have unit "case" and some are "CS". Also "each" variants
		if (inUom == null)
			revisedUom = "?uom";
		else revisedUom = UomNormalizer.normalizeString(inUom);
		return inItemMasterId + "-" + inLocation.getNominalLocationId() + "-" + revisedUom;
	}

	public Item() {
	}

	public Item(ItemMaster parent, String domainId) {
		super(domainId);
		setParent(parent);
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<Item> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "IT";
	}

	@ManyToOne
	public final String getItemId() {
		return parent.getItemId();
	}

	public final void setStoredLocation(final ILocation<?> inStoredLocation) {
		// If it's already in another location then remove it from that location.
		// Shall we use its existing domainID (which will change momentarily?
		// Or compute what its domainID must have been in that location?
		if (storedLocation != null) {
			storedLocation.removeStoredItemFromMasterIdAndUom(getItemId(), getUomMasterId());
		}
		storedLocation = (LocationABC<?>) inStoredLocation;
		// The stored location is part of the domain key for an item's instance.
		setDomainId(makeDomainId(getItemId(), inStoredLocation, getUomMasterId()));
		inStoredLocation.addStoredItem(this);
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	// Assorted meta fields for the UI
	public final String getNominalLocationId() {
		ILocation<?> theLoc = getStoredLocation();
		if (theLoc == null)
			return "";
		else {
			return theLoc.getNominalLocationId();
		}
	}

	public final String getItemLocationAlias() {
		ILocation<?> theLoc = getStoredLocation();
		if (theLoc == null)
			return "";
		else {
			return theLoc.getPrimaryAliasId();
		}
	}

	public void setItemLocationAlias(String inLocationAliasId) {
		LocationAlias alias = getParent().getParent().getLocationAlias(inLocationAliasId);
		if (alias == null) {
			throw new DaoException("could not find location with alias: " + inLocationAliasId);
		}
		setStoredLocation(alias.getMappedLocation());
	}

	public final String getItemCmFromLeft() {
		Integer value = getCmFromLeft();
		if (value != 0)
			return value.toString();
		else {
			return "";
		}
	}
	
	public final String getItemMasterId() {
		ItemMaster master = getParent();
		return master.getDomainId();
	}

	public final void setItemCmFromLeft(String inValueFromLeft) {
		Integer positionValue;
		if (Strings.isNullOrEmpty(inValueFromLeft) || inValueFromLeft.trim().length() == 0) {
			positionValue = null;
		}
		else {
			positionValue = Integer.valueOf(inValueFromLeft);
		}
		setPositionFromLeft(positionValue);
	}

	public final String getPosAlongPathui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPosAlongPath());
	}

	public final String getItemTier() {
		// The intended purpose is allow user to filter by aisle, then sort by tier and getPosAlongPathui
		//  Should allow easy verification of inventory, working across a tier.
		String result = "";
		SubLocationABC<?> location = (SubLocationABC<?>) this.getStoredLocation();
		if (location != null) {
			LocationABC<?> tierLocation = (LocationABC<?>) location.getParentAtLevel(Tier.class);
			if (tierLocation != null) {
				result = tierLocation.getDomainId();
			}
		}
		return result;
	}

	public final String getItemDescription() {
		ItemMaster theMaster = this.getParent();
		if (theMaster == null)
			return "";
		else {
			return theMaster.getDescription();
		}
	}
	
	public final String getUomMasterId() {
		// uom is not nullable, but we see null in unit test.
		UomMaster theUom = getUomMaster();
		if (theUom != null)
			return theUom.getDomainId();
		else {
			// what to do? Let's use the default for the item master
			LOGGER.error("null uom on item in getUomMasterId");
			ItemMaster theMaster = this.getParent();
			if (theMaster == null)
				return "";
			else {
				return "";
				// standardUom is private. Could make public.
				// return theMaster.getStandardUom.getDomainId();
			}
		}
	}


	// UI Metafield
	public final String getItemQuantityUom() {

		Double quant = this.getQuantity();
		UomMaster theUom = this.getUomMaster();
		if (theUom == null || quant == null)
			return "";

		String uom = theUom.getDomainId();
		String quantStr;

		// for each or case, make sure we do not return the foolish looking 2.0 EA.
		
		//It was deemed that zero in the system is the unknown quantity in this system 
		//   the system is not inventory tracking system so zero quantity in item does not mean you 
		//   are out of stock
		if (Math.abs(quant.doubleValue() - 0.0d) < 0.000001) {//zero essentially
			quantStr = "?";
			
		} else if (uom.equalsIgnoreCase("EA") || uom.equalsIgnoreCase("CS")) {
			quantStr = Integer.toString((int) quant.doubleValue());
		} else {
			quantStr = Double.toString(quant);
		}
		quantStr = Strings.padEnd(quantStr, 3, ' ');
		return quantStr + " " + uom;
	}

	// Public setter/getter/validate functions for our cmFromLeft feature
	public final void setPositionFromLeft(Integer inCmFromLeft) {
		if (inCmFromLeft == null) {
			setMetersFromAnchor(null);
		}
		else {
			Double value = 0.0;
			ILocation<?> theLocation = this.getStoredLocation();
			Double pickEnd = ((SubLocationABC<?>) theLocation).getPickFaceEndPosX();
			if (pickEnd == 0.0)
				pickEnd = ((SubLocationABC<?>) theLocation).getPickFaceEndPosY();
			if (theLocation.isLeftSideTowardsAnchor()) {
				value = inCmFromLeft / 100.0;
			} else {
				value = pickEnd - (inCmFromLeft / 100.0);
			}
			if (value > pickEnd) {
				LOGGER.error("setPositionFromLeft value out of range");
				value = pickEnd;
			}
			if (value < 0.0) {
				LOGGER.error("ssetPositionFromLeft value out of range");
				value = 0.0;
			}
			setMetersFromAnchor(value);
		}
	}

	public final String validatePositionFromLeft(ILocation<?> inLocation, Integer inCmFromLeft) {
		String result = "";
		if (inLocation == null) {
			result = "Unknown location";
		} else if (inCmFromLeft != null && inCmFromLeft < 0) {
			// if the cm value is wider than that bay/tier, or negative
			result = "Negative cm value not allowed";
		} else {
			Double pickEnd = ((SubLocationABC<?>) inLocation).getPickFaceEndPosX();
			if (pickEnd != null) {
				if (pickEnd == 0.0)
					pickEnd = ((SubLocationABC<?>) inLocation).getPickFaceEndPosY();
				if (pickEnd != null) {
					if (inCmFromLeft != null) {
						if (pickEnd < inCmFromLeft / 100.0) {
							result = "Cm value too large. Location is not that wide.";
						}
					} else {
						LOGGER.error("inCmFromLeft was null");
						result = "invalid null value (inCmFromLeft)";
					}
				} else {
					LOGGER.error("getPickFaceEndPosY returned null");
					result = "invalid null value (getPickFaceEndPosY)";
				}
			} else {
				LOGGER.error("getPickFaceEndPosX returned null");
				result = "invalid null value (getPickFaceEndPosX)";
			}
		}
		return result;

	}

	public Integer getCmFromLeft() {
		Double meters = getMetersFromAnchor();
		if (meters == null || meters == 0.0)
			return 0;

		Integer value = 0;

		ILocation<?> theLocation = this.getStoredLocation();
		if (theLocation.isLeftSideTowardsAnchor()) {
			value = (int) Math.round(meters * 100.0);
		} else { // cm back from the pickface end
			Double pickEnd = ((SubLocationABC<?>) theLocation).getPickFaceEndPosX();
			if (pickEnd == 0.0)
				pickEnd = ((SubLocationABC<?>) theLocation).getPickFaceEndPosY();
			if (pickEnd < meters) {
				LOGGER.error("Bug found: getCmFromLeft in non-slotted inventory model");
				value = (int) Math.round(pickEnd * 100.0);
			} else {
				value = (int) Math.round((pickEnd - meters) * 100.0);
			}
		}

		return value;
	}

	// This mimics the old getter, but now is done via a computation. This is the key routine.
	// May well be worth caching this value. Only changes if item's location changes, metersFromAnchor changes, or path change.
	public Double getPosAlongPath() {
		ILocation<?> theLocation = this.getStoredLocation();
		Double locationPosValue = theLocation.getPosAlongPath();
		Double returnValue = locationPosValue;
		if (returnValue == null) {
			// should only happen if path is not set yet. Should it return 0.0?
			return null;
		}
		Double meters = getMetersFromAnchor();
		if (meters == null) {
			return locationPosValue;
		}
		if (meters == 0.0) // we can skip the complications
			return locationPosValue;
		// 2 cases.
		// - path increasing from anchor, so location's posAlongPath reflects the anchor.
		// - path decreasing from anchor, so location's posAlongPath reflects its pickEndPos.
		
		// We need to know if the location's anchor is up or down the path.
		// If we have a meters value, we are adding, because the location's value is its first edge along the path; the item is futher along with any offset at all.
		if (theLocation.isPathIncreasingFromAnchor())
			returnValue += meters;
		else {
			// if path opposing the anchor, then the pickface end corresponds to the location value. We have to convert before adding.
			Double pickEnd = ((SubLocationABC<?>) theLocation).getPickFaceEndPosX();
			if (pickEnd == 0.0)
				pickEnd = ((SubLocationABC<?>) theLocation).getPickFaceEndPosY();
			if (pickEnd < meters) {
				LOGGER.error("Bug found: Item.getPosAlongPath in non-slotted inventory model");
				// let it return the location's posAlongPath
			} else {
				Double correctedMeters = pickEnd - meters;
				returnValue += correctedMeters;
			}
		}

		// It should be true that no item in a location can ever have lower posAlongPath value than the location itself.
		// This is required for cart runs, or else scanned start location for cart my not include work instructions in the location.
		if (returnValue < locationPosValue) {
			LOGGER.error("suspect item meters along path calculation");
			returnValue = locationPosValue;
		}

		return returnValue;
	}

	// This mimics the old setter, but now would be done via a computation.
	public void setPosAlongPath(Double inDdcPosition) {
		setMetersFromAnchor(0.0);
		// only called by facility putDdcItemsInPositionOrder(), which is probably never used now.
		// Used to be used by an old stitchfix DDC case. Compute, and immediately set inventory each time. 
		// We probably will not do it this way now that we have a good non-slotted model.
		// getPosAlongPath at least will return the location's value.
	}
	
	public LedRange getFirstLastLedsForItem() {
		LedRange theLedRange = new LedRange();
		
		// to compute, we need the locations first and last led positions
		ILocation<?> theLocation = this.getStoredLocation();
		int firstLocLed = theLocation.getFirstLedNumAlongPath(); 
		int lastLocLed = theLocation.getLastLedNumAlongPath(); 
		// following cast not safe if the stored location is facility
		if (theLocation.getClass() == Facility.class)
			return theLedRange; // was initialized to give values of 0,0
		
		Double metersFromAnchor = getMetersFromAnchor();
		
		Double locationWidth = ((SubLocationABC<?>) theLocation).getLocationWidthMeters();
		boolean lowerLedNearAnchor = theLocation.isLowerLedNearAnchor();
		
		theLedRange.computeLedsToLight(firstLocLed, lastLocLed, locationWidth, metersFromAnchor, lowerLedNearAnchor);
		
		return theLedRange;
	}
	
	public String getLitLedsForItem(){
		LedRange theRange = getFirstLastLedsForItem();
		return (theRange.getRangeString());
	}

}
