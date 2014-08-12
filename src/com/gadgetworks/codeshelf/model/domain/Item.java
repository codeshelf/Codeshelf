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
	private Double				posAlongPath;

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

	public final String getItemCmFromLeft() {
		Integer value = getCmFromLeft();
		if (value != 0)
			return value.toString();
		else {
			return "";
		}
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
		LocationABC tierLocation = (LocationABC) location.getParentAtLevel(Tier.class);
		if (tierLocation != null)
			result = tierLocation.getDomainId();
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
	public final void setPositionFromLeft(LocationABC inLocation, Integer inCmFromLeft) {
		if (inLocation != null)
			// this is the original behavior
			setPosAlongPath(inLocation.getPosAlongPath());
	}

	public final String validatePositionFromLeft(LocationABC inLocation, Integer inCmFromLeft) {
		String result = "";
		if (inLocation == null)
			result = "Unknown location";
		// if the cm value is wider than that bay/tier, or negative
		if (inCmFromLeft < 0)
			result = "Negative cm value not allowed";
		return result;
		// 

	}

	public Integer getCmFromLeft() {
		Integer value = 0;
		return value;
		// if this item's stored location getPosAlongPath() == this.getPosAlongPath(), then cm is 0.
	}

}
