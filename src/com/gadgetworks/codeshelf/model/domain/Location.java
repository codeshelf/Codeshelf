/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationABC.java,v 1.40 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.device.LedCmdPath;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.util.StringUIConverter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

// --------------------------------------------------------------------------
/**
 * The anchor point and vertex collection to define the planar space of a work structure (e.g. facility, bay, shelf, etc.)
 */

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "location")
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class Location extends DomainObjectTreeABC<Location> {

	static Double BAY_ALIGNMENT_FUDGE = 0.25;
	
	// This really should somehow include the space between the bay if there are gaps in a long row with certain kinds of LED strips.
	// For example, the current strips are spaced exactly 3.125cm apart.
	public static final Double										METERS_PER_LED_POS	= 0.03125;

	private static final Logger										LOGGER				= LoggerFactory.getLogger(Location.class);

	// The position type (GPS, METERS, etc.).
	@Column(nullable = false,name="anchor_pos_type")
	@Enumerated(value = EnumType.STRING)
	@JsonProperty
	@Getter
	private PositionTypeEnum										anchorPosType;

	// The X anchor position.
	@Column(nullable = false,name="anchor_pos_x")
	@JsonProperty
	@Getter
	private Double													anchorPosX;

	// The Y anchor position.
	@Column(nullable = false,name="anchor_pos_y")
	@JsonProperty
	@Getter
	private Double													anchorPosY;

	// The Z anchor position.
	@Column(nullable = false,name="anchor_pos_z")
	@JsonProperty
	@Getter
	private Double													anchorPosZ;

	// The location description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String													description;

	// How far this location is from the path's origin.
	@Column(nullable = true,name="pos_along_path")
	@Getter
	@Setter
	@JsonProperty
	private Double													posAlongPath;

	// Associated path segment (optional)
	@ManyToOne(optional = true)
	@JoinColumn(name="path_segment_persistentid")
	@Getter
	@Setter
	private PathSegment												pathSegment;
	// The getter is renamed getAssociatedPathSegment, which still looks up the parent chain until it finds a pathSegment.
	// DomainObjectABC will manufacture a call to getPathSegment during DAO.store(). So do not skip the getter with complicated overrides

	// The LED controller.
	@ManyToOne(optional = true)
	@JoinColumn(name="led_controller_persistentid")
	@Getter
	@Setter
	private LedController											ledController;

	// The LED controller's channel that lights this location.
	@Column(nullable = true,name="led_channel")
	@Getter
	@Setter
	private Short													ledChannel;

	// The bay's first LED position on the channel.
	@Column(nullable = true,name="first_led_num_along_path")
	@Getter
	@Setter
	private Short													firstLedNumAlongPath;

	// The number of LED positions in the bay.
	@Column(nullable = true,name="last_led_num_along_path")
	@Getter
	@Setter
	private Short													lastLedNumAlongPath;

	// The first DDC ID for this location (if it has one).
	@Column(nullable = true,name="first_ddc_id")
	@Getter
	@Setter
	@JsonProperty
	private String													firstDdcId;

	// The last DDC ID for this location (if it has one).
	@Column(nullable = true,name="last_ddc_id")
	@Getter
	@Setter
	@JsonProperty
	private String													lastDdcId;

	// All of the vertices that define the location's footprint.
	@OneToMany(mappedBy = "parent")
	@Getter
	@Setter
	private List<Vertex>											vertices			= new ArrayList<Vertex>();

	// The child locations.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, Location>	locations			= new HashMap<String, Location>();

	// The location aliases for this location.
	@OneToMany(mappedBy = "mappedLocation")
	@Getter
	@Setter
	private List<LocationAlias>										aliases				= new ArrayList<LocationAlias>();

	// The items stored in this location.
	@OneToMany(mappedBy = "storedLocation")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, Item>										storedItems			= new HashMap<String, Item>();

	// The DDC groups stored in this location.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, ItemDdcGroup>								itemDdcGroups		= new HashMap<String, ItemDdcGroup>();

	// For this location, is the lower led on the anchor side?
	@Column(nullable = true,name="lower_led_near_anchor")
	@Getter
	@Setter
	private Boolean													lowerLedNearAnchor;

	// Is this location active?
	@Column(nullable = false)
	@Getter
	@Setter
	private Boolean													active;

	public Location() {
		active = true;

		this.setAnchorPoint(Point.getZeroPoint());
		this.setPickFaceEndPoint(Point.getZeroPoint());
	}

	public Location(String domainId, final Point inAnchorPoint) {
		super(domainId);
		active = true;
		setAnchorPoint(inAnchorPoint);
		this.setPickFaceEndPoint(Point.getZeroPoint());
	}

	public void updateAnchorPoint(Double x, Double y, Double z) {
		anchorPosX = x;
		anchorPosY = y;
		anchorPosZ = z;
		this.getDao().store(this);
	}

	public Point getAnchorPoint() {
		return new Point(anchorPosType, anchorPosX, anchorPosY, anchorPosZ);
	}

	public final void setAnchorPoint(final Point inAnchorPoint) {
		anchorPosType = inAnchorPoint.getPosType();
		anchorPosX = inAnchorPoint.getX();
		anchorPosY = inAnchorPoint.getY();
		anchorPosZ = inAnchorPoint.getZ();
	}

	public final void setAnchorPosTypeByStr(final String inPosType) {
		anchorPosType = PositionTypeEnum.valueOf(inPosType);
	}

	public final List<Location> getChildren() {
		return new ArrayList<Location>(locations.values());
	}

	public final List<Location> getActiveChildren() {
		ArrayList<Location> aList = new ArrayList<Location>();
		for (Location loc : locations.values()) {
			if (loc.isActive())
				aList.add(loc);
		}
		return aList;
	}

	// --------------------------------------------------------------------------
	/*
	 * this is the "delete" method. Does not delete. Merely makes inactive, along with all its children.
	 * This does the DAO persist.
	 */
	public void makeInactiveAndAllChildren() {
		this.setActive(false);
		try {
			this.getDao().store(this);
		} catch (DaoException e) {
			LOGGER.error("makeInactive", e);
		}

		List<Location> childList = getActiveChildren();
		for (Location sublocation : childList) {
			sublocation.makeInactiveAndAllChildren();
		}
	}

	// --------------------------------------------------------------------------
	/* getActiveChildrenAtLevel should only be called for active locations. By our model, it should be impossible for active child to have inactive parents.
	 * See CD_0051 Delete Locations. Once inactive child is encountered, it does not look further down that child chain. Also, will not return itself if inactive and of the right class
	 */
	// --------------------------------------------------------------------------
	/**
	 * Get all of the children of this type (no matter how far down the hierarchy).
	 * 
	 * To get it to strongly type the return for you then use this unusual Java construct at the caller:
	 * 
	 * Aisle aisle = facility.<Aisle> getActiveChildrenAtLevel(Aisle.class)
	 * (If calling this method from a generic location type then you need to define it as LocationABC<?> location.)
	 * 
	 * @param inClassWanted
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T extends Location> List<T> getActiveChildrenAtLevel(Class<? extends Location> inClassWanted) {
		List<T> result = new ArrayList<T>();
		if (!this.isActive()) {
			LOGGER.error("getActiveChildrenAtLevel called for inactive location");
			return result;
		}

		// Loop through all of the active children.
		for (Location child : getActiveChildren()) {
			if (child.getClass().equals(inClassWanted)) {
				// If the child is the kind we want then add it to the list.
				result.add((T) child);
			} else {
				// If the child is not the kind we want the recurse.
				result.addAll((List<T>) child.getActiveChildrenAtLevel(inClassWanted));
			}
		}

		// If this class is also in the class we want then add it.
		// While it's not technically its own child, we are looking for this type.
		if (getClass().equals(inClassWanted)) {
			result.add((T) this);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getLocationIdToParentLevel(java.lang.Class)
	 */
	public final String getLocationIdToParentLevel(Class<? extends Location> inClassWanted) {
		String result;

		// It seems reasonable in the code to ask for getLocationIdToParentLevel(Aisle.class) when the class of the object is unknown, and might even be the facility.
		// Let's not NPE.
		if (this.getClass().equals(Facility.class))
			return "";
		else if (this.getClass().equals(inClassWanted)) {
			return getLocationId();
		}

		Location checkParent = getParent();
		if (checkParent.getClass().equals(inClassWanted)) {
			// This is the parent we want.
			result = checkParent.getLocationId() + "." + getLocationId();
		} else {
			if (checkParent.getClass().equals(Facility.class)) {
				// We cannot go higher than the Facility as a parent, so there is no such parent with the requested class.
				result = "";
			} else {
				// The current parent is not the class we want so recurse up the hierarchy.
				result = checkParent.getLocationIdToParentLevel(inClassWanted);
				result = result + "." + getLocationId();
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* Returns something like A2.B1.T3.S5. Needs to call recursively up the chain.
	 * This is private helper for getNominalLocationId()
	 *
	 */
	public final String getNominalLocationIdExcludeBracket() {
		String result;

		// It seems reasonable in the code to ask for getLocationIdToParentLevel(Aisle.class) when the class of the object is unknown, and might even be the facility.
		// Let's not NPE.
		if (this.getClass().equals(Facility.class))
			return "";

		Location checkParent = (Location) getParent();
		if (checkParent.getClass().equals(Facility.class)) {
			// This is the last child  we want.
			result = getLocationId();
		} else {
			// The current parent is not the class we want so recurse up the hierarchy.
			result = checkParent.getNominalLocationIdExcludeBracket();
			result = result + "." + getLocationId();
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* Returns something like A2.B1.T3.S5. Or if the location is inactive, <A2.B1.T3.S5>
	 *
	 */
	public final String getNominalLocationId() {
		String result = getNominalLocationIdExcludeBracket();
		if (!this.isActive())
			result = "<" + result + ">";
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Get the parent of this location at the class level specified.
	 * 
	 * To get it to strongly type the return for you then use this unusual Java construct at the caller:
	 * 
	 * Aisle aisle = bay.<Aisle> getParentAtLevel(Aisle.class)
	 * (If calling this method from a generic location type then you need to define it as LocationABC<?> location.)
	 * 
	 * @param inClassWanted
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T extends Location> T getParentAtLevel(Class<? extends Location> inClassWanted) {

		// if you call aisle.getParentAtLevel(Aisle.class), return itself. This is moderately common.
		if (this.getClass().equals(inClassWanted))
			return (T) this; // (We can cast safely since we checked the class.)

		T result = null;

		Location checkParent = getParent();
		if (checkParent != null) {
			if (checkParent.getClass().equals(inClassWanted)) {
				// This is the parent we want. (We can cast safely since we checked the class.)
				result = (T) checkParent;
			} else {
				if (checkParent.getClass().equals(Facility.class)) {
					// We cannot go higher than the Facility as a parent, so there is no such parent with the requested class.
					result = null;
				} else {
					// The current parent is not the class we want so recurse up the hierarchy.
					result = (T) checkParent.getParentAtLevel(inClassWanted);
				}
			}
		} else {
			LOGGER.error("parent location of: " + this + " could not be retrieved");
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getAbsolutePosX()
	 */
	public Point getAbsoluteAnchorPoint() {
		Point anchor = getAnchorPoint();
		Point result = anchor;
		if (!anchorPosType.equals(PositionTypeEnum.GPS)) {
			Location parent = getParent();
			if (parent != null && parent.getAnchorPoint().getPosType().equals(PositionTypeEnum.METERS_FROM_PARENT)) {
				result = anchor.add(parent.getAbsoluteAnchorPoint());
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getLocationId()
	 */
	public final String getLocationId() {
		return getDomainId();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#setLocationId(java.lang.String)
	 */
	public final void setLocationId(final String inLocationId) {
		setDomainId(inLocationId);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#addLocation(com.gadgetworks.codeshelf.model.domain.SubLocationABC)
	 */
	public final void addLocation(Location inLocation) {
		IDomainObject oldParent = inLocation.getParent();
		if (oldParent == null) {
			locations.put(inLocation.getDomainId(), inLocation);
			inLocation.setParent(this);
		} else if (!oldParent.equals(this)) {
			LOGGER.error("cannot add Location " + inLocation.getDomainId() + " to " + this.getClassName() + " "
					+ this.getDomainId() + " because it has not been removed from " + oldParent.getDomainId());
		}
	}

	// --------------------------------------------------------------------------
	/*
	 * Normally called via facility.findSubLocationById(). If no dots in the name, calls this, which first looks for alias, but only if at the facility level.
	 * Aside from the facility/alias special case, it looks directly for domainId, which may be useful only for looking for direct children.
	 * for example, facility.findLocationById("A2") would find the aisle.
	 * bay bay.findLocationById("T3") would find the tier.
	 * This may return null
	 */
	public final Location findLocationById(String inLocationId) {
		if (this.getClass().equals(Facility.class)) {
			Facility facility = (Facility)this;
			LocationAlias alias = facility.getLocationAlias(inLocationId);
			if ((alias != null) && (alias.getActive())) {
				return alias.getMappedLocation();
			}
		} // else
		return locations.get(inLocationId);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#removeLocation(java.lang.String)
	 */
	public final void removeLocation(String inLocationId) {
		Location location = locations.get(inLocationId);
		if (location != null) {
			location.setParent(null);
			locations.remove(inLocationId);
		} else {
			LOGGER.error("cannot remove SubLocationABC " + inLocationId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	// --------------------------------------------------------------------------
	/*
	 * Normally called as facility.findSubLocationById(). If no dots in the name, calls findLocationById(), which first looks for alias, but only if at the facility level.
	 * This will return "deleted" location, but if it does so, it will log a WARN.
	 */
	// --------------------------------------------------------------------------
	/**
	 * Look for any sub-location by it's ID.
	 * The location ID needs to be a dotted notation where the first octet is a child location of "this" location.
	 * @param inLocationId
	 * @return
	 */
	public final Location findSubLocationById(final String inLocationId) {
		Location result = null;

		Integer firstDotPos = inLocationId.indexOf(".");
		if (firstDotPos < 0) {
			// There's no "dot" so look for the sublocation at this level.
			result = this.findLocationById(inLocationId);
		} else {
			// There is a dot, so find the sublocation based on the first part and recursively ask it for the location from the second part.
			String firstPart = inLocationId.substring(0, firstDotPos);
			String secondPart = inLocationId.substring(firstDotPos + 1);
			Location firstPartLocation = this.findLocationById(firstPart);
			if (firstPartLocation != null) {
				result = firstPartLocation.findSubLocationById(secondPart);
			}
		}
		if (result != null)
			if (!result.isActive())
				LOGGER.warn("findSubLocationById succeeded with an inactive location. Is this business case intentional?");
		return result;
	}

	public final PathSegment getAssociatedPathSegment() {
		PathSegment result = null;

		if (pathSegment == null) {
			Location parent = (Location) getParent();
			if (parent != null) {
				result = parent.getAssociatedPathSegment();
			}
		} else {
			result = pathSegment;
		}

		return result;
	}

	public final String getPathSegId() {
		// to support list view meta-field pathSegId
		PathSegment aPathSegment = getAssociatedPathSegment();

		if (aPathSegment != null) {
			return aPathSegment.getDomainId();
		}
		return "";
	}

	/**
	 * This gets the associated channel. Or if indirect via getEffectiveController, then parenthesis around it.
	 */
	public final String getLedChannelUi() {
		Short theValue = getLedChannel();
		if (theValue != null) {
			return theValue.toString();
		} else {
			theValue = getEffectiveLedChannel();
			if (theValue != null)
				return "(" + theValue.toString() + ")";
		}
		return "";
	}

	/**
	 * This directly get the associated controller only
	 */
	public final String getLedControllerId() {
		// to support list view meta-field ledControllerId
		LedController aLedController = getLedController();

		if (aLedController != null) {
			return aLedController.getDomainId();
		}
		return "";
	}

	/**
	 * This gets the associated controller. Or if indirect via getEffectiveController, then parenthesis around it.
	 */
	public final String getLedControllerIdUi() {
		LedController aLedController = getLedController();
		if (aLedController != null) {
			return aLedController.getDomainId();
		} else {
			aLedController = getEffectiveLedController();
			if (aLedController != null)
				return "(" + aLedController.getDomainId() + ")";
		}
		return "";
	}

	public final String getVerticesUi() {
		// A UI meta field. Mostly for developers as we refine our graphic ui
		List<Vertex> vList = getVerticesInOrder();
		String returnStr = "";
		for (Vertex vertex : vList) {
			// we want to assemble "(xvalue,yvalue)" for each vertex in order
			String vString = "(";
			vString += StringUIConverter.doubleToTwoDecimalsString(vertex.getPosX());
			vString += ", ";
			vString += StringUIConverter.doubleToTwoDecimalsString(vertex.getPosY());
			vString += ") ";
			returnStr += vString;
		}
		return returnStr;
	}

	public final String getPrimaryAliasId() {
		// to support list view meta-field
		LocationAlias theAlias = getPrimaryAlias();
		if (theAlias != null) {
			if (this.isActive())
				return theAlias.getAlias();
			else
				return "<" + theAlias.getAlias() + ">"; // new from v8. If location is inactive, surround with <>
		}
		return "";
	}

	public final void addVertex(Vertex inVertex) {
		Location previousLocation = inVertex.getParent();
		if (previousLocation == null) {
			vertices.add(inVertex);
			inVertex.setParent(this);
		} else if (!previousLocation.equals(this)) {
			LOGGER.error("cannot add Vertex " + inVertex.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousLocation.getDomainId());
		}
	}

	public final void removeVertex(Vertex inVertex) {
		if (this.vertices.contains(inVertex)) {
			inVertex.setParent(null);
			vertices.remove(inVertex);
		} else {
			LOGGER.error("cannot remove Vertex " + inVertex.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void addAlias(LocationAlias inAlias) {
		Location previousLocation = inAlias.getMappedLocation();
		if (previousLocation == null) {
			aliases.add(inAlias);
			inAlias.setMappedLocation(this);
		} else if (!previousLocation.equals(this)) {
			LOGGER.error("cannot map Alias " + inAlias.getDomainId() + " to " + this.getDomainId()
					+ " because it is still mapped to " + previousLocation.getDomainId());
		}
	}

	public final void removeAlias(LocationAlias inAlias) {
		if (this.aliases.contains(inAlias)) {
			inAlias.setMappedLocation(null);
			aliases.remove(inAlias);
		} else {
			LOGGER.error("cannot unmap Alias " + inAlias.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in aliases");
		}
	}

	public final LocationAlias getPrimaryAlias() {
		LocationAlias result = null;

		for (LocationAlias alias : aliases) {
			if (alias.getActive()) {
				result = alias;
				break;
			}
		}

		return result;
	}

	public final void addStoredItem(Item inItem) {
		Location previousLocation = inItem.getStoredLocation();

		// If it's already in another location then remove it from that location.
		// Shall we use its existing domainID (which will change momentarily?
		// Or compute what its domainID must have been in that location?
		if (previousLocation != null) {
			previousLocation.removeStoredItem(inItem);
			previousLocation = null;
		}

		if (previousLocation == null) {
			String domainId = Item.makeDomainId(inItem.getItemId(), this, inItem.getUomMasterId());
			inItem.setDomainId(domainId);
			// why not just ask the item for its domainId? The item's domainID is changing as it moves.
			storedItems.put(domainId, inItem);
			inItem.setStoredLocation(this);
		} /*else if(!previousLocation.equals(this)) {
			LOGGER.error("cannot addStoredItem "+inItem.getDomainId()+" to "+this.getDomainId()+" because it is still stored at "+previousLocation.getDomainId());
			}	*/

	}

	public final Item getStoredItemFromMasterIdAndUom(final String inItemMasterId, final String inUom) {
		String domainId = Item.makeDomainId(inItemMasterId, this, inUom);
		// why not just ask the item for its domainId?

		Item returnItem = storedItems.get(domainId);
		return returnItem;
	}

	public final Item getStoredItemFromLocationAndMasterIdAndUom(final String inLocationName,
		final String inItemMasterId,
		final String inUom) {
		Item returnItem = null;
		Location location = this.findSubLocationById(inLocationName);
		if (location != null)
			returnItem = location.getStoredItemFromMasterIdAndUom(inItemMasterId, inUom);
		return returnItem;
	}

	public final Item getStoredItem(final String inItemDomainId) {
		return storedItems.get(inItemDomainId);
	}

	public final void removeStoredItem(Item inItem) {
		String itemDomainId = inItem.getDomainId();
		Item storedItem = storedItems.get(itemDomainId);
		if (storedItem != null) {
			storedItem.setStoredLocation(null);
			storedItems.remove(itemDomainId);
			return;
		}
		// If not found, two possibilities of bug. (Or inconsistent data after unexpected throw.)
		// 1) Call remove from a location that does not have the item.
		// 2) The location does have the item, but mapped under a different domainId. This is the bug I want to look for.

		for (Item iterItem : storedItems.values()) {
			if (iterItem.equals(inItem)) {
				storedItem = iterItem;
				LOGGER.error("removeStoredItem  found" + itemDomainId + " in " + this.getDomainId()
						+ " but not keyed by domainId correctly");  // This is bug 2)
				break;
			}
		}
		
		if (storedItem != null) {
			storedItem.setStoredLocation(null);
			// How do we remove it from stored items?
		} else {
			LOGGER.error("cannot removeStoredItem " + itemDomainId + " from " + this.getDomainId()
					+ " because it isn't found in children"); // This is bug 1)
		}
	}

	public final void addItemDdcGroup(ItemDdcGroup inItemDdcGroup) {
		Location previousLocation = inItemDdcGroup.getParent();
		if (previousLocation == null) {
			itemDdcGroups.put(inItemDdcGroup.getDdcGroupId(), inItemDdcGroup);
			inItemDdcGroup.setParent(this);
		} else if (!previousLocation.equals(this)) {
			LOGGER.error("cannot addItemDdcGroup " + inItemDdcGroup.getDomainId() + " to " + this.getDomainId()
					+ " because it is still stored at " + previousLocation.getDomainId());
		}

	}

	public final ItemDdcGroup getItemDdcGroup(final String inItemDdcGroupId) {
		return itemDdcGroups.get(inItemDdcGroupId);
	}

	public final void removeItemDdcGroup(final String inItemDdcGroupId) {
		ItemDdcGroup itemDdcGroup = itemDdcGroups.get(inItemDdcGroupId);
		if (itemDdcGroup != null) {
			itemDdcGroup.setParent(null);
			itemDdcGroups.remove(inItemDdcGroupId);
		} else {
			LOGGER.error("cannot removeItemDdcGroup " + inItemDdcGroupId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final List<ItemDdcGroup> getDdcGroups() {
		return new ArrayList<ItemDdcGroup>(itemDdcGroups.values());
	}

	public final Short getFirstLedPosForItemId(final String inItemId) {
		Short result = 0;

		Item item = this.getStoredItem(inItemId);
		if (item != null) {
			ItemDdcGroup ddcGroup = getItemDdcGroup(item.getParent().getDdcId());
			if (ddcGroup != null) {
				result = getLedNumberFromPosAlongPath(ddcGroup.getStartPosAlongPath());
			} else {
				result = getFirstLedNumAlongPath();
			}
		}

		return result;
	}

	public final Short getLastLedPosForItemId(final String inItemId) {
		Short result = 0;

		Item item = this.getStoredItem(inItemId);
		if (item != null) {
			ItemDdcGroup ddcGroup = getItemDdcGroup(item.getParent().getDdcId());
			if (ddcGroup != null) {
				result = getLedNumberFromPosAlongPath(ddcGroup.getEndPosAlongPath());
			} else {
				result = getLastLedNumAlongPath();
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Given a position along the path (that should be within this location) return the LED closest to the position.
	 * @param inPosAlongPath
	 * @return
	 */
	private Short getLedNumberFromPosAlongPath(Double inPosAlongPath) {
		Short result = 0;

		// Right now the LEDs are a fixed 3.125cm per position (for now).
		// In the future we'll need to know what kind of device is mounted onto the location.

		Double diffToPosition = inPosAlongPath - getPosAlongPath();
		Short ledCountFromEdge = (short) Math.round(diffToPosition / METERS_PER_LED_POS);

		if (getFirstLedNumAlongPath() < getLastLedNumAlongPath()) {
			result = (short) (getFirstLedNumAlongPath() + ledCountFromEdge);
		} else {
			result = (short) (getFirstLedNumAlongPath() - ledCountFromEdge);
		}

		return result;
	}

	/**
	 * Compare locations by their position relative to each other.
	 *
	 */
	public static class LocationWorkingOrderComparator implements Comparator<Location> {

		public int compare(Location inLoc1, Location inLoc2) {
			if (inLoc1.getAnchorPosZ() > inLoc2.getAnchorPosZ()) {
				return -1;
			} else if (inLoc1.getPosAlongPath() == null || inLoc2.getPosAlongPath() == null) {
				LOGGER.error("posAlongPath null for location in LocationWorkingOrderComparator");
				;
				return 0;
			} else if (inLoc1.getPosAlongPath() < inLoc2.getPosAlongPath()) {
				return -1;
			}
			return 1;
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * Get all of the sublocations (down the the very tree bottom) of this location, in working order.
	 * The working order gets applied to each level down from this location.
	 * Working order is top-to-bottom and then down-path.
	 * @return
	 */
	public List<Location> getSubLocationsInWorkingOrder() {
		List<Location> result = new ArrayList<Location>();
		List<Location> childLocations = getActiveChildren();
		Collections.sort(childLocations, new LocationWorkingOrderComparator());
		for (Location childLocation : childLocations) {
			// add sublocation
			result.add(childLocation);
			// and its sublocations recursively
			result.addAll(childLocation.getSubLocationsInWorkingOrder());
		}
		return result;
	}


	// --------------------------------------------------------------------------
	/**
	 * Get all of the children (one level down) of this location, in working order.
	 * Working order is top-to-bottom and then down-path.
	 * @return
	 */
	public List<Location> getChildrenInWorkingOrder() {
		List<Location> childLocations = getActiveChildren();
		Collections.sort(childLocations, new LocationWorkingOrderComparator());
		return childLocations;
	}

	private class VertexOrderComparator implements Comparator<Vertex> {

		public int compare(Vertex inVertex1, Vertex inVertex2) {
			if (inVertex1.getDrawOrder() > inVertex2.getDrawOrder()) {
				return 1;
			} else if (inVertex1.getDrawOrder() < inVertex2.getDrawOrder()) {
				return -1;
			}
			return 0;
		}
	};

	public final List<Vertex> getVerticesInOrder() {

		List<Vertex> result = getVertices();
		Collections.sort(result, new VertexOrderComparator());

		return result;
	}

	public final LedController getEffectiveLedController() {
		// See if we have the controller. Then recursively ask each parent until found.
		LedController theController = getLedController();
		if (theController == null) {
			Location aLocation = (Location) this.getParent();
			if (aLocation != null) {
				theController = aLocation.getEffectiveLedController();
			}
		}

		return theController;
	}

	public final Short getEffectiveLedChannel() {
		// See if we have the controller. Then recursively ask each parent until found.
		Short theChannel = getLedChannel();
		if (theChannel == null) {
			Location aLocation = (Location) this.getParent();
			if (aLocation != null) {
				theChannel = aLocation.getEffectiveLedChannel();
			}
		}
		return theChannel;
	}

	public Set<LedCmdPath> getAllLedCmdPaths() {
		Set<LedCmdPath> cmdPathsSet = new HashSet<LedCmdPath>();
		if (getEffectiveLedController() != null) {
			cmdPathsSet.add(new LedCmdPath(getEffectiveLedController().getDeviceGuidStr(), getEffectiveLedChannel()));
		} else {
			for (Location child : getActiveChildren()) {
				cmdPathsSet.addAll(child.getAllLedCmdPaths());
			}
		}
		return cmdPathsSet;
	}

	public final Boolean isLeftSideTowardsAnchor() {
		// As you face the pickface, is the left toward the anchor (where the B1/S1 side is)
		Aisle theAisle = this.getParentAtLevel(Aisle.class);
		if (theAisle == null) {
			return false;
		} else {
			return theAisle.isLeftSideAsYouFaceByB1S1();
			// this is moderately expensive, and rarely changes. Cache after first computation? Used extensively in item cmFromLeft calculations
		}
	}

	public final Boolean isPathIncreasingFromAnchor() {
		Aisle theAisle = this.getParentAtLevel(Aisle.class);
		if (theAisle == null) {
			return true; // as good a default as any
		} else {
			return theAisle.associatedPathSegmentIncreasesFromAnchor();
			// this is moderately expensive, and rarely changes. Cache after first computation? Used in item.getPosAlongPath computation
		}
	}

	public final Boolean isLowerLedNearAnchor() {
		// This answer may not be meaningful for some locations, such as for a bay with zigzag led pattern.
		Boolean answer = getLowerLedNearAnchor();
		return (answer == null || answer);
		// Could this have been computed instead of persisted?
		// The only way would have been to examine siblings of this location. Or children if there are any.
		// Not siblings with the same parent, but same level tier for adjacent bay. Note that zigzags are tricky.
	}

	public final Boolean isActive() {
		return getActive();
	}

	@JsonIgnore
	public boolean isLightable() {
		LedController controller = this.getEffectiveLedController();
		Short controllerChannel = this.getEffectiveLedChannel();
		if (controller == null || controllerChannel == null) {
			return false;
		}
		Short firstLocLed = getFirstLedNumAlongPath();
		Short lastLocLed = getLastLedNumAlongPath();
		if (firstLocLed == null || lastLocLed == null) {
			return false;
		} else if (firstLocLed == 0 && lastLocLed == 0) {
			return false;
		}
		return true;
	}

	public LedRange getFirstLastLedsForLocation() {
		// This often returns the stated leds for slots. But if the span is large, returns the central 4 leds.
		// to compute, we need the locations first and last led positions
		Short firstLocLed = getFirstLedNumAlongPath();
		Short lastLocLed = getLastLedNumAlongPath();
		if (firstLocLed == null || lastLocLed == null) {
			throw new UnsupportedOperationException(String.format("Cannot calculate LedRange for %s, firstLed: %s , lastLed %s ",
				this,
				firstLocLed,
				lastLocLed));
		}
		// following cast not safe if the stored location is facility
		if (this instanceof Facility)
			return LedRange.zero(); // was initialized to give values of 0,0

		boolean lowerLedNearAnchor = this.isLowerLedNearAnchor();

		LedRange theLedRange = LedRange.computeLedsToLightForLocationNoOffset(firstLocLed, lastLocLed, lowerLedNearAnchor);

		return theLedRange;
	}

	private class InventoryPositionComparator implements Comparator<Item> {
		// We want this to sort from low to high
		public int compare(Item item1, Item item2) {
			int result = ComparisonChain.start()
				.compare(item1.getPosAlongPath(), item2.getPosAlongPath(), Ordering.natural().nullsLast())
				.result();
			return result;
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Gets inventory for this instance and all of its descendents then sorts along path.
	 * Public mainly for testability
	 *
	 */
	public List<Item> getInventoryInWorkingOrder() {
		ArrayList<Item> aList = Lists.newArrayList();
		// Add my inventory
		aList.addAll(getStoredItems().values());
		// Add my children's inventory
		for (Location location : locations.values()) {
			aList.addAll(location.getInventoryInWorkingOrder());
		}
		// Sort as we want it
		Collections.sort(aList, new InventoryPositionComparator());

		return aList;
	}

	public Facility getFacility() {
		return getParent().getFacility();
	}

	// The owning location.
	@Getter
	@ManyToOne
	protected Location parent;

	@Column(nullable = false,name="pick_face_end_pos_type")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PositionTypeEnum	pickFaceEndPosType;

	// X pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false,name="pick_face_end_pos_x")
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosX;

	// Y pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false,name="pick_face_end_pos_y")
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosY;

	// Z pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false,name="pick_face_end_pos_z")
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosZ;
		
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.SubLocationABC#setParent(P)
	 */
	public void setParent(Location inParent) {
		parent = inParent;
	}

	public Point getAbsolutePickFaceEndPoint() {
		Point base = getAbsoluteAnchorPoint();
		return base.add(getPickFaceEndPosX(), getPickFaceEndPosY(), getPickFaceEndPosZ());
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public Point getPickFaceEndPoint() {
		return new Point(pickFaceEndPosType, pickFaceEndPosX, pickFaceEndPosY, pickFaceEndPosZ);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPickFaceEndPoint
	 */
	public void setPickFaceEndPoint(final Point inPickFaceEndPoint) {
		pickFaceEndPosType = inPickFaceEndPoint.getPosType();
		pickFaceEndPosX = inPickFaceEndPoint.getX();
		pickFaceEndPosY = inPickFaceEndPoint.getY();
		pickFaceEndPosZ = inPickFaceEndPoint.getZ();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#computePosAlongPath(com.gadgetworks.codeshelf.model.domain.PathSegment)
	 */
	public final void computePosAlongPath(final PathSegment inPathSegment) {
		if (inPathSegment == null) {
			LOGGER.error("null pathSegment in computePosAlongPath");
			return;
		}

		// Complete revision at V4
		Point locationAnchorPoint = getAbsoluteAnchorPoint();
		Point pickFaceEndPoint = getAbsolutePickFaceEndPoint();
		// The location's posAlongPath is the lower of the anchor or pickFaceEnd
		Double locAnchorPathPosition = inPathSegment.computeNormalizedPositionAlongPath(locationAnchorPoint);
		Double pickFaceEndPathPosition = inPathSegment.computeNormalizedPositionAlongPath(pickFaceEndPoint);
		Double newPosition = Math.min(locAnchorPathPosition, pickFaceEndPathPosition);
		Double oldPosition = this.getPosAlongPath();

		// Doing this to avoid the DAO needing to check the change, which also generates a bunch of logging.
		if (!newPosition.equals(oldPosition)) {
			try {
				LOGGER.debug(this.getFullDomainId() + " path pos: " + getPosAlongPath() + " Anchor x: "
						+ locationAnchorPoint.getX() + " y: " + locationAnchorPoint.getY() + " Face x: ");
				setPosAlongPath(newPosition);
				this.getDao().store(this);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		// Also force a recompute for all of the child locations.
		List<Location> locations = getActiveChildren();
		for (Location location : locations) {
			location.computePosAlongPath(inPathSegment);
		}
	}

	/**
	 * Clears controller and channel back to null state, as if they had never been set after initialization
	 */
	public final void clearControllerChannel() {
		if (getLedController() != null || getLedChannel() != null) {
			try {
				LedController currentController = this.getLedController();
				if (currentController != null)
					currentController.removeLocation(this);
				this.setLedChannel(null);
				this.getDao().store(this);
			} catch (DaoException e) {
				LOGGER.error("doClearControllerChannel", e);
			}
		}
	}

	/**
	 * Both Aisle and Tier have setControllerChannel functions that call through to this.
	 * Side effect adds a little complexity. If setting to a valid controller, make sure there is a channel (default 1) even if never set to user set to 0.
	 */
	protected final void doSetControllerChannel(String inControllerPersistentIDStr, String inChannelStr) {

		// Initially, log
		LOGGER.debug("On " + this + ", set LED controller to " + inControllerPersistentIDStr);

		// Get the LedController
		UUID persistentId = UUID.fromString(inControllerPersistentIDStr);
		LedController theLedController = LedController.DAO.findByPersistentId(persistentId);

		// set this tier's
		if (theLedController != null) {
			// Get the channel
			Short theChannel;
			try {
				theChannel = Short.valueOf(inChannelStr);
			} catch (NumberFormatException e) {
				theChannel = 0; // not recognizable as a number
			}
			if (theChannel < 0) {
				theChannel = 0; // means don't change if there is a channel. Or set to 1 if there isn't.
			}

			// set the controller. And set the channel
			theLedController.addLocation(this);
			if (theChannel != null && theChannel > 0) {
				this.setLedChannel(theChannel);
			} else {
				// if channel passed is 0 or null Short, make sure tier has a ledChannel. Set to 1 if there is not yet a channel.
				Short thisLedChannel = this.getLedChannel();
				if (thisLedChannel == null || thisLedChannel <= 0)
					this.setLedChannel((short) 1);
			}

			this.getDao().store(this);
		} else {
			throw new DaoException("Unable to set controller, controller " + inControllerPersistentIDStr + " not found");
		}
	}

	// converts A3 into 003.  Could put the A back on.  Could be a static, but called this way conveniently from tier and from bay
	public String getCompString(String inString) {
		String s = inString.substring(1); // Strip off the A, B, T, or S
		// we will pad with leading spaces to 3
		int padLength = 3;
		int needed = padLength - s.length();
		if (needed <= 0) {
			return s;
		}
		char[] padding = new char[needed];
		java.util.Arrays.fill(padding, '0');
		StringBuffer sb = new StringBuffer(padLength);
		sb.append(padding);
		sb.append(s);
		String ss = sb.toString();
		return ss;
	}

	public Double getLocationWidthMeters() {
		// Seems funny, but it is so. Anchor is relative to parent. PickFaceEnd is relative to anchor.
		// So, the width is just the pickface end value.
		if (isLocationXOriented())
			return this.getPickFaceEndPosX();
		else
			return this.getPickFaceEndPosY();
	}

	public boolean isLocationXOriented() {
		return getPickFaceEndPosY() == 0.0;
	}

	// UI fields
	public String getAnchorPosXui() {
		return StringUIConverter.doubleToTwoDecimalsString(getAnchorPosX());
	}

	public String getAnchorPosYui() {
		return StringUIConverter.doubleToTwoDecimalsString(getAnchorPosY());
	}

	public String getAnchorPosZui() {
		return StringUIConverter.doubleToTwoDecimalsString(getAnchorPosZ());
	}

	public String getPickFaceEndPosXui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPickFaceEndPosX());
	}

	public String getPickFaceEndPosYui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPickFaceEndPosY());
	}

	public String getPickFaceEndPosZui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPickFaceEndPosZ());
	}

	public String getPosAlongPathui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPosAlongPath());
	}


}


