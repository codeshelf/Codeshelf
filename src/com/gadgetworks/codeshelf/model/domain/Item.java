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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.DaoException;
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
@Table(name = "item")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString
public class Item extends DomainObjectTreeABC<ItemMaster> {

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
	@Setter
	private ItemMaster			parent;

	// The stored location.
	@Column(nullable = false)
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
	@Column(nullable = false)
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
	public static String makeDomainId(final String inItemId, final ILocation<?> inLocation) {
		// as soon as we have "lot" field on item, we want to either pass the lot in, or get from the item.
		// an item is defined unique combination of item master, lot, and location.
		return inItemId + "-" + inLocation.getNominalLocationId();
		/*
		return inItemId + "-" + inLocation.getLocationId();
		*/
	}

	public Item() {
	}

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

	public final void setStoredLocation(final ILocation inStoredLocation) {
		// If it's already in another location then remove it from that location.
		if (storedLocation != null) {
			storedLocation.removeStoredItem(getItemId());
		}
		storedLocation = (LocationABC) inStoredLocation;
		// The stored location is part of the domain key for an item's instance.
		setDomainId(makeDomainId(getItemId(), inStoredLocation));
		inStoredLocation.addStoredItem(this);
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	// Assorted meta fields for the UI
	public final String getNominalLocationId() {
		LocationABC theLoc = getStoredLocation();
		if (theLoc == null)
			return "";
		else {
			return theLoc.getNominalLocationId();
		}
	}

	public final String getItemLocationAlias() {
		LocationABC theLoc = getStoredLocation();
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
		setPositionFromLeft(Integer.valueOf(inValueFromLeft));
	}

	public final String getPosAlongPathui() {
		// for the moment, just return the location's value.
		SubLocationABC location = (SubLocationABC) this.getStoredLocation();
		return location.getPosAlongPathui();
		// later, need to adjust for cmFromLeft
	}

	public final String getItemTier() {
		// The intended purpose is allow user to filter by aisle, then sort by tier and getPosAlongPathui
		//  Should allow easy verification of inventory, working across a tier.
		String result = "";
		SubLocationABC location = (SubLocationABC) this.getStoredLocation();
		if (location != null) {
			LocationABC tierLocation = (LocationABC) location.getParentAtLevel(Tier.class);
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

	interface Padder {
		String padRight(String inString, int inPadLength);
	}

	public final String getItemQuantityUom() {

		// All to have access to a trivial padRight function. Could just in line.
		// This is done as an anonymous class. Will like move to some utility class someday
		Padder padder = new Padder() {
			public String padRight(String inString, int inPadLength) {
				String str = inString;
				for (int i = inString.length(); i <= inPadLength; i++) {
					str += " ";
				}
				return str;

			}
		};

		Double quant = this.getQuantity();
		UomMaster theUom = this.getUomMaster();
		if (theUom == null || quant == null)
			return "";

		String uom = theUom.getDomainId();
		String quantStr;
		// for each or case, make sure we do not return the foolish looking 2.0 EA.
		if (uom.equalsIgnoreCase("EA") || uom.equalsIgnoreCase("CS")) {
			quantStr = Integer.toString((int) quant.doubleValue());
		} else {
			quantStr = Double.toString(quant);
		}
		quantStr = padder.padRight(quantStr, 3);
		return quantStr + " " + uom;
	}

	// Public setter/getter/validate functions for our cmFromLeft feature
	public final void setPositionFromLeft(Integer inCmFromLeft) {
		Double value = 0.0;
		LocationABC theLocation = this.getStoredLocation();
		Double pickEnd = ((SubLocationABC) theLocation).getPickFaceEndPosX();
		if (pickEnd == 0.0)
			pickEnd = ((SubLocationABC) theLocation).getPickFaceEndPosY();
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

	public final String validatePositionFromLeft(LocationABC inLocation, Integer inCmFromLeft) {
		String result = "";
		if (inLocation == null)
			result = "Unknown location";
		// if the cm value is wider than that bay/tier, or negative
		if (inCmFromLeft < 0)
			result = "Negative cm value not allowed";
		Double pickEnd = ((SubLocationABC) inLocation).getPickFaceEndPosX();
		if (pickEnd == 0.0)
			pickEnd = ((SubLocationABC) inLocation).getPickFaceEndPosY();
		if (pickEnd < inCmFromLeft / 100.0) {
			result = "Cm value too large. Location is not that wide.";
		}
		return result;

	}

	public Integer getCmFromLeft() {
		Double meters = getMetersFromAnchor();
		if (meters == null || meters == 0.0)
			return 0;

		Integer value = 0;

		LocationABC theLocation = this.getStoredLocation();
		if (theLocation.isLeftSideTowardsAnchor()) {
			value = (int) Math.round(meters * 100.0);
		} else { // cm back from the pickface end
			Double pickEnd = ((SubLocationABC) theLocation).getPickFaceEndPosX();
			if (pickEnd == 0.0)
				pickEnd = ((SubLocationABC) theLocation).getPickFaceEndPosY();
			if (pickEnd < meters) {
				LOGGER.error("obvious bug in getCmFromLeft and non-slotted inventory model");
				value = (int) Math.round(pickEnd * 100.0);
			} else {
				value = (int) Math.round((pickEnd - meters) * 100.0);
			}
		}

		return value;
	}

	// This mimics the old getter, but now is done via a computation.
	public Double getPosAlongPath() {
		Double returnValue = 0.0;

		LocationABC theLocation = this.getStoredLocation();
		returnValue = theLocation.getPosAlongPath();
		// Now we need to add or subtract the bit corresponding to getMetersFromAnchor. 
		// Is left going up or down the path? That depends on direction of the local pathSegment, and getMetersFromAnchor.
		// NOT DONE YET

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

}
