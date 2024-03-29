/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Item.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.LedRange;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.StringUIConverter;
import com.codeshelf.util.UomNormalizer;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

// --------------------------------------------------------------------------
/**
 * Item
 * 
 * An instance of an item in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "item", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString
public class Item extends DomainObjectTreeABC<ItemMaster> {

	public static class ItemDao extends GenericDaoABC<Item> implements ITypedDao<Item> {
		public final Class<Item> getDaoClass() {
			return Item.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Item.class);

	// The stored location.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "stored_location_persistentid")
	@Getter
	private Location			storedLocation;

	// Quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				quantity;

	// The actual UoM.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "uom_master_persistentid")
	@Getter
	@Setter
	private UomMaster			uomMaster;

	// Ddc position.
	@Column(nullable = true, name = "meters_from_anchor")
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
	public Item() {
	}

	public Item(ItemMaster itemMaster, UomMaster uomMaster) {
		setUomMaster(uomMaster);
		itemMaster.addItemToMaster(this);
	}

	/**
	 * This creates a standard domainId that keeps all of the items in different locations unique among a single ItemMaster.
	 * @param inItemId
	 * @param inLocationId
	 * @return
	 */
	public static String makeDomainId(final String inItemMasterId, final Location inLocation, final String inUom) {
		// as soon as we have "lot" field on item, we want to either pass the lot in, or get from the item.
		// an item is defined unique combination of item master, lot, and location.
		String revisedUom = inUom;
		// Let's not be confused if some items have unit "case" and some are "CS". Also "each" variants
		if (inUom == null)
			revisedUom = "?uom";
		else
			revisedUom = UomNormalizer.normalizeString(inUom);
		return inItemMasterId + "-" + inLocation.getNominalLocationIdExcludeBracket() + "-" + revisedUom;
	}

	public String makeDomainId() {
		return makeDomainId(getItemMasterId(), getStoredLocation(), getUomMasterId());
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Item> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<Item> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Item.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "IT";
	}

	public String getItemId() {
		return getParent().getItemId();
	}

	public void setStoredLocation(final Location inStoredLocation) {
		storedLocation = inStoredLocation;
	}

	// Assorted meta fields for the UI
	public String getNominalLocationId() {
		Location theLoc = getStoredLocation();
		if (theLoc == null)
			return "";
		else {
			return theLoc.getNominalLocationId();
		}
	}

	// We want this to return the nominal name if there is no alias.
	public String getItemLocationName() {
		Location theLoc = getStoredLocation();
		if (theLoc == null)
			return "";
		else {
			return theLoc.getBestUsableLocationName();
		}
	}

	// For symmetry with getItemLocationName, this must accept alias or nominal name
	// Careful, this is called by webapp for direct editing of item's location.
	public void setItemLocationName(String inAliasOrNominal) {
		LocationAlias alias = LocationAlias.staticGetDao().findByDomainId(getParent().getParent(), inAliasOrNominal);
		Location loc = null;
		if (alias != null) {
			loc = alias.getMappedLocation();
		} else {
			Facility facility = this.getFacility();
			loc = facility.findSubLocationById(inAliasOrNominal);
		}
		if (loc == null) {
			throw new DaoException("could not find location named: " + inAliasOrNominal);
		}
		if (!loc.isActive()) {
			throw new DaoException("The location with alias: " + inAliasOrNominal + " was deleted");
		}
		setStoredLocation(loc);
	}

	public String getItemMasterId() {
		ItemMaster master = getParent();
		return master.getDomainId();
	}

	/**
	 * Property that uses String type
	 * @return
	 */
	public String getCmFromLeftui() {
		Integer value = getCmFromLeft();
		if (value != 0)
			return value.toString();
		else {
			return "";
		}
	}

	/**
	 * Property that uses String type
	 * @return
	 */
	public void setCmFromLeftui(String inValueFromLeft) {
		Integer positionValue = null;
		if (Strings.isNullOrEmpty(inValueFromLeft) || inValueFromLeft.trim().length() == 0) {
			positionValue = null;
		} else {
			try {
				positionValue = Integer.valueOf(inValueFromLeft);
			} catch (NumberFormatException e) {
				throw new InputValidationException(this, "cmFromLeft", inValueFromLeft, ErrorCode.FIELD_WRONG_TYPE);
			}
		}
		setCmFromLeft(positionValue);
	}

	public String getPosAlongPathui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPosAlongPath());
	}

	public String getItemTier() {
		// The intended purpose is allow user to filter by aisle, then sort by tier and getPosAlongPathui
		//  Should allow easy verification of inventory, working across a tier.
		String result = "";
		Location location = this.getStoredLocation();
		if (location != null) {
			Location tierLocation = location.getParentAtLevel(Tier.class);
			if (tierLocation != null) {
				result = tierLocation.getDomainId();
			}
		}
		return result;
	}

	public String getItemDescription() {
		ItemMaster theMaster = this.getParent();
		if (theMaster == null)
			return "";
		else {
			return theMaster.getDescription();
		}
	}

	public String getUomMasterId() {
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
	public String getItemQuantityUom() {

		Double quant = this.getQuantity();
		UomMaster theUom = this.getUomMaster();
		if (theUom == null || quant == null)
			return "";

		String uom = theUom.getDomainId();
		String quantStr;

		// for each or case, make sure we do not return the foolish looking 2.0 EA.
		// UomNormalizer equivalents also return a 2 PK.  Other units sill may look foolish, such as
		// 3.0 box. We would need the table of Uom to know if the units are discrete or not.

		//It was deemed that zero in the system is the unknown quantity in this system 
		//   the system is not inventory tracking system so zero quantity in item does not mean you 
		//   are out of stock
		if (Math.abs(quant.doubleValue() - 0.0d) < 0.000001) {//zero essentially
			quantStr = "?";

		} else if (UomNormalizer.normalizedEquals(uom, UomNormalizer.CASE)
				|| UomNormalizer.normalizedEquals(uom, UomNormalizer.EACH)) {
			quantStr = Integer.toString((int) quant.doubleValue());
		} else {
			quantStr = Double.toString(quant);
		}
		quantStr = Strings.padEnd(quantStr, 3, ' ');
		return quantStr + " " + uom;
	}

	// Public setter/getter/validate functions for our cmFromLeft feature
	public void setCmFromLeft(Integer inCmFromLeft) {
		if (inCmFromLeft == null) {
			setMetersFromAnchor(null);
		} else {
			Double value = 0.0;
			Location theLocation = this.getStoredLocation();
			if (!theLocation.isFacility()) {
				if (inCmFromLeft < 0) {
					throw new InputValidationException(this, "cmFromLeft", inCmFromLeft, ErrorCode.FIELD_NUMBER_NOT_NEGATIVE);
				} else if (theLocation != null && !theLocation.isFacility()) {
					Double pickEndWidthMeters = theLocation.getLocationWidthMeters();
					if (theLocation.isLeftSideTowardsB1S1()) {
						value = inCmFromLeft / 100.0;
					} else {
						value = pickEndWidthMeters - (inCmFromLeft / 100.0);
					}
					if (value > pickEndWidthMeters || value < 0) {
						throw new InputValidationException(this, "cmFromLeft", inCmFromLeft, ErrorCode.FIELD_NUMBER_ABOVE_MAX);
					}
				}
				setMetersFromAnchor(value);
			} else {
				LOGGER.debug("unexpected setCmFromLeft on facility");
			}
		}
	}

	public Integer getCmFromLeft() {
		Double meters = getMetersFromAnchor();
		if (meters == null || meters == 0.0)
			return 0;

		Integer value = 0;

		Location theLocation = this.getStoredLocation();
		if (theLocation.isLeftSideTowardsB1S1()) {
			value = (int) Math.round(meters * 100.0);
		} else { // cm back from the pickface end
			Double pickEndWidthMeters = theLocation.getLocationWidthMeters();
			if (pickEndWidthMeters < meters) {
				LOGGER.error("Bug found: getCmFromLeft in non-slotted inventory model");
				value = (int) Math.round(pickEndWidthMeters * 100.0);
			} else {
				value = (int) Math.round((pickEndWidthMeters - meters) * 100.0);
			}
		}

		return value;
	}

	// This mimics the old getter, but now is done via a computation. This is the key routine.
	// May well be worth caching this value. Only changes if item's location changes, metersFromAnchor changes, or path change.
	public Double getPosAlongPath() {
		Location theLocation = this.getStoredLocation();
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
			Double pickEndWidthMeters = theLocation.getLocationWidthMeters();
			if (pickEndWidthMeters < meters) {
				LOGGER.error("Bug found: Item.getPosAlongPath in non-slotted inventory model");
				// let it return the location's posAlongPath
			} else {
				Double correctedMeters = pickEndWidthMeters - meters;
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
		// to compute, we need the locations first and last led positions
		Location theLocation = this.getStoredLocation();
		if (theLocation.isFacility())
			return LedRange.zero(); // was initialized to give values of 0,0

		int firstLocLed = theLocation.getFirstLedNumAlongPath();
		int lastLocLed = theLocation.getLastLedNumAlongPath();

		Double metersFromAnchor = getMetersFromAnchor();

		Double locationWidth = theLocation.getLocationWidthMeters();
		boolean lowerLedNearAnchor = theLocation.isLowerLedNearAnchor();

		LedRange theLedRange = LedRange.computeLedsToLight(firstLocLed,
			lastLocLed,
			lowerLedNearAnchor,
			locationWidth,
			metersFromAnchor,
			theLocation.isSlot());
		return theLedRange;
	}

	public String getLitLedsForItem() {
		LedRange theRange = getFirstLastLedsForItem();
		return (theRange.getRangeString());
	}

	public Facility getFacility() {
		return getParent().getFacility();
	}

	public boolean isLightable() {
		Location itemLocation = getStoredLocation();
		return (itemLocation != null && itemLocation.isLightable());
	}

	public String toLogString() {
		// Let's return much less than the lomboc toString.
		String toSay = "Sku:" + this.getItemMasterId() + " Uom:" + this.getUomMasterId() + " Loc:" + this.getItemLocationName();
		return toSay;
	}

	public Gtin getGtin() {
		UomMaster uomMaster = getUomMaster();

		if (uomMaster != null) {
			return getParent().getGtinForUom(uomMaster);
		} else {
			return null;
		}
	}

	public String getGtinId() {
		UomMaster uomMaster = getUomMaster();
		Gtin gtin = null;

		if (uomMaster != null) {
			gtin = getParent().getGtinForUom(uomMaster);
			if (gtin != null) {
				return gtin.getDomainId();
			} else {
				return "";
			}
		} else {
			LOGGER.error("UOM Master for {} was null.", this.getDomainId());
			return "";
		}
	}
}
