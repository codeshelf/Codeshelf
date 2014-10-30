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
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.device.LedCmdPath;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.util.StringUIConverter;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * LocationABC
 *
 * The anchor point and vertex collection to define the planar space of a work structure (e.g. facility, bay, shelf, etc.)
 *
 * NB: We can't use bean cache for location because SubLocation exists to make it so that a Facility doesn't have to have a parent location.
 * The problem with that is that cachebeans get stored with properties from their highest class (not all class in the hierarchy), so
 * the "parent" property is often not available (to the LocationABC root location object).  There are two ways to fix this:
 *
 * 1. Go back and get rid of SubLocationABC and make Facility be its own parent so that we can enfore parent constraint on all location.
 * 2. Fix ebean caches to be a bit smarter and bring in all properties for a location.
 *
 * There is a possibility that we could make SubLocationABC's parent a SubLocation and then the bean cache would always pull in
 * the SubLocationClass (instead of LocationABC), but there's some weird thing the causes a ClassCastException when the setParent()
 * gets called.  If that we fixable it might be a good way to go.
 *
 * @author jeffw
 */

@Entity
@MappedSuperclass
@CacheStrategy(useBeanCache = false)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "location")
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class LocationABC<P extends IDomainObject> extends DomainObjectTreeABC<P> implements ILocation<P> {

	@SuppressWarnings("rawtypes")
	@Inject
	public static ITypedDao<LocationABC>	DAO;

	@SuppressWarnings("rawtypes")
	@Singleton
	public static class LocationABCDao extends GenericDaoABC<LocationABC> implements ITypedDao<LocationABC> {

		// We include the IDatabase arg to cause Guice to initialize it *before* locations.
		@Inject
		public LocationABCDao(final ISchemaManager inSchemaManager, final IDatabase inDatabase) {
			super(inSchemaManager);
		}

		public final Class<LocationABC> getDaoClass() {
			return LocationABC.class;
		}
	}

	// This really should somehow include the space between the bay if there are gaps in a long row with certain kinds of LED strips.
	// For example, the current strips are spaced exactly 3.125cm apart.
	public static final Double			METERS_PER_LED_POS	= 0.03125;

	private static final Logger			LOGGER				= LoggerFactory.getLogger(LocationABC.class);

	//	@Embedded
	//	@AttributeOverrides({ @AttributeOverride(name = "x", column = @Column(name = "anchor_pos_x")),
	//		@AttributeOverride(name = "y", column = @Column(name = "anchor_pos_y")),
	//		@AttributeOverride(name = "z", column = @Column(name = "anchor_pos_z")),
	//		@AttributeOverride(name = "posTypeEnum", column = @Column(name = "anchor_pos_type_enum")) })
	//	@Getter
	//	@Setter
	//	@JsonProperty
	//	private Point						anchorPos;

	// The position type (GPS, METERS, etc.).
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@JsonProperty
	@Getter
	private PositionTypeEnum			anchorPosTypeEnum;

	// The X anchor position.
	@Column(nullable = false)
	@JsonProperty
	@Getter
	private Double						anchorPosX;

	// The Y anchor position.
	@Column(nullable = false)
	@JsonProperty
	@Getter
	private Double						anchorPosY;

	// The Z anchor position.
	@Column(nullable = false)
	@JsonProperty
	@Getter
	private Double						anchorPosZ;

	// The location description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	// How far this location is from the path's origin.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Double						posAlongPath;

	// Associated path segment (optional)
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private PathSegment					pathSegment;
	// The getter is renamed getAssociatedPathSegment, which still looks up the parent chain until it finds a pathSegment.
	// DomainObjectABC will manufacture a call to getPathSegment during DAO.store(). So do not skip the getter with complicated overrides

	//	// The owning organization.
	//	@Column(nullable = true)
	//	@ManyToOne(optional = true)
	//	@Getter
	//	@Setter
	//	private Organization				parentOrganization;
	//
	// The LED controller.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private LedController				ledController;

	// The LED controller's channel that lights this location.
	@Column(nullable = true)
	@Getter
	@Setter
	private Short						ledChannel;

	// The bay's first LED position on the channel.
	@Column(nullable = true)
	@Getter
	@Setter
	private Short						firstLedNumAlongPath;

	// The number of LED positions in the bay.
	@Column(nullable = true)
	@Getter
	@Setter
	private Short						lastLedNumAlongPath;

	// The first DDC ID for this location (if it has one).
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						firstDdcId;

	// The last DDC ID for this location (if it has one).
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						lastDdcId;

	// All of the vertices that define the location's footprint.
	@OneToMany(mappedBy = "parent")
	@Getter
	@Setter
	private List<Vertex>				vertices			= new ArrayList<Vertex>();

	// The child locations.
	@SuppressWarnings("rawtypes")
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, SubLocationABC>	locations			= new HashMap<String, SubLocationABC>();

	// The location aliases for this location.
	@OneToMany(mappedBy = "mappedLocation")
	@Getter
	@Setter
	private List<LocationAlias>			aliases				= new ArrayList<LocationAlias>();

	// The items stored in this location.
	@OneToMany(mappedBy = "storedLocation")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, Item>			storedItems			= new HashMap<String, Item>();

	// The DDC groups stored in this location.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, ItemDdcGroup>	itemDdcGroups		= new HashMap<String, ItemDdcGroup>();

	// For this location, is the lower led on the anchor side?
	@Column(nullable = true)
	@Getter
	@Setter
	private Boolean						lowerLedNearAnchor;

	// Is this location active?
	@Column(nullable = false)
	@Getter
	@Setter
	private Boolean						active;

	public LocationABC() {
		active = true;
	}

	public LocationABC(String domainId, final Point inAnchorPoint) {
		super(domainId);
		active = true;
		setAnchorPoint(inAnchorPoint);
	}

	public void updateAnchorPoint(Double x, Double y, Double z) {
		anchorPosX = x;
		anchorPosY = y;
		anchorPosZ = z;
		DAO.store(this);
	}

	public Point getAnchorPoint() {
		return new Point(anchorPosTypeEnum, anchorPosX, anchorPosY, anchorPosZ);
	}

	public final void setAnchorPoint(final Point inAnchorPoint) {
		anchorPosTypeEnum = inAnchorPoint.getPosTypeEnum();
		anchorPosX = inAnchorPoint.getX();
		anchorPosY = inAnchorPoint.getY();
		anchorPosZ = inAnchorPoint.getZ();
	}

	public final void setAnchorPosTypeByStr(final String inPosType) {
		anchorPosTypeEnum = PositionTypeEnum.valueOf(inPosType);
	}

	@SuppressWarnings("rawtypes")
	public final List<ISubLocation> getChildren() {
		return new ArrayList<ISubLocation>(locations.values());
	}

	@SuppressWarnings("rawtypes")
	public final List<ISubLocation> getActiveChildren() {
		ArrayList<ISubLocation> aList = new ArrayList<ISubLocation>();
		for (ISubLocation loc : locations.values()) {
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
	@SuppressWarnings("rawtypes")
	public void makeInactiveAndAllChildren() {
		this.setActive(false);
		try {
			DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("makeInactive", e);
		}

		List<ISubLocation> childList = getActiveChildren();
		for (ISubLocation sublocation : childList) {
			((LocationABC) sublocation).makeInactiveAndAllChildren();
		}
	}

	// --------------------------------------------------------------------------
	/* getActiveChildrenAtLevel should only be called for active locations. By our model, it should be impossible for active child to have inactive parents.
	 * See CD_0051 Delete Locations. Once inactive child is encountered, it does not look further down that child chain. Also, will not return itself if inactive and of the right class
	 */
	@SuppressWarnings("unchecked")
	public final <T extends ISubLocation<?>> List<T> getActiveChildrenAtLevel(Class<? extends ISubLocation<?>> inClassWanted) {
		List<T> result = new ArrayList<T>();
		if (!this.isActive()) {
			LOGGER.error("getActiveChildrenAtLevel called for inactive location");
			return result;
		}

		// Loop through all of the active children.
		for (ISubLocation<?> child : getActiveChildren()) {
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
	 * @see com.gadgetworks.codeshelf.model.domain.ILocation#getLocationIdToParentLevel(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public final String getLocationIdToParentLevel(Class<? extends ILocation<?>> inClassWanted) {
		String result;

		ILocation<P> checkParent = (ILocation<P>) getParent();

		// It seems reasonable in the code to ask for getLocationIdToParentLevel(Aisle.class) when the class of the object is unknown, and might even be the facility.
		// Let's not NPE.
		if (this.getClass().equals(Facility.class))
			return "";
		else if (this.getClass().equals(inClassWanted)) {
			return getLocationId();
		}

		// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
		// This fixes that problem, but it's not pretty.
		checkParent = DAO.findByPersistentId(checkParent.getClass(), checkParent.getPersistentId());

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
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.ILocation#getLocationIdToParentLevel(java.lang.Class)
	 */
	public final String getNominalLocationId() {
		String result;

		// It seems reasonable in the code to ask for getLocationIdToParentLevel(Aisle.class) when the class of the object is unknown, and might even be the facility.
		// Let's not NPE.
		if (this.getClass().equals(Facility.class))
			return "";

		@SuppressWarnings("unchecked")
		ILocation<P> checkParent = (ILocation<P>) getParent();
		if (checkParent.getClass().equals(Facility.class)) {
			// This is the last child  we want.
			result = getLocationId();
		} else {
			// The current parent is not the class we want so recurse up the hierarchy.
			result = checkParent.getNominalLocationId();
			result = result + "." + getLocationId();
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getParentAtLevel(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public final <T extends ILocation<?>> T getParentAtLevel(Class<? extends ILocation<?>> inClassWanted) {

		// if you call aisle.getParentAtLevel(Aisle.class), return itself. This is moderately common.
		if (this.getClass().equals(inClassWanted))
			return (T) this; // (We can cast safely since we checked the class.)

		T result = null;

		ILocation<P> checkParent = (ILocation<P>) getParent();
		if (checkParent != null) {
			// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
			// This fixes that problem, but it's not pretty.
			UUID persistentId = checkParent.getPersistentId();
			checkParent = DAO.findByPersistentId(checkParent.getClass(), persistentId);
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
				LOGGER.error("parent location of: " + this + " could not be retrieved with id: " + persistentId);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.ILocation#getAbsolutePosX()
	 */
	@SuppressWarnings("unchecked")
	public Point getAbsoluteAnchorPoint() {
		Point anchor = getAnchorPoint();
		Point result = anchor;
		if (!anchorPosTypeEnum.equals(PositionTypeEnum.GPS)) {
			ILocation<P> parent = (ILocation<P>) getParent();

			// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
			// This fixes that problem, but it's not pretty.
			parent = DAO.findByPersistentId(parent.getClass(), parent.getPersistentId());
			if ((parent != null) && (parent.getAnchorPoint().getPosTypeEnum().equals(PositionTypeEnum.METERS_FROM_PARENT))) {
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
	public final void addLocation(ISubLocation<?> inLocation) {
		// Ebean can't deal with interfaces.
		SubLocationABC<?> subLocation = (SubLocationABC<?>) inLocation;
		locations.put(inLocation.getDomainId(), subLocation);
	}

	// --------------------------------------------------------------------------
	/* 
	 * Normally called via facility.findSubLocationById(). If no dots in the name, calls this, which first looks for alias, but only if at the facility level.
	 * Aside from the facility/alias special case, it looks directly for domainId, which may be useful only for looking for direct children.
	 * for example, facility.findLocationById("A2") would find the aisle.
	 * bay bay.findLocationById("T3") would find the tier.
	 * This may return null
	 */
	public final ISubLocation<?> findLocationById(String inLocationId) {
		// There's some ebean weirdness around Map caches, so we have to use a different strategy to resolve this request.
		//return locations.get(inLocationId);
		ISubLocation<?> result = null;

		// If the current location is a facility then first look for an alias
		if (this.getClass().equals(Facility.class)) {
			Facility facility = (Facility) this;
			LocationAlias alias = facility.getLocationAlias(inLocationId);
			if ((alias != null) && (alias.getActive())) {
				return alias.getMappedLocation();
			}
		}

		// We didn't find an alias, so search through DB for the matching location.
		@SuppressWarnings("rawtypes")
		ITypedDao<SubLocationABC> dao = SubLocationABC.DAO;
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("persistentId", this.getPersistentId().toString());
		filterParams.put("domainId", inLocationId);

		@SuppressWarnings("rawtypes")
		List<SubLocationABC> resultSet = dao.findByFilter("parent.persistentId = :persistentId and domainId = :domainId",
			filterParams);
		if ((resultSet != null) && (resultSet.size() > 0)) {
			result = resultSet.get(0);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#removeLocation(java.lang.String)
	 */
	public final void removeLocation(String inLocationId) {
		locations.remove(inLocationId);
	}

	// --------------------------------------------------------------------------
	/* 
	 * Normally called as facility.findSubLocationById(). If no dots in the name, calls findLocationById(), which first looks for alias, but only if at the facility level.
	 * This will return "deleted" location, but if it does so, it will log a WARN.
	 */
	public final ISubLocation<?> findSubLocationById(final String inLocationId) {
		ISubLocation<?> result = null;

		Integer firstDotPos = inLocationId.indexOf(".");
		if (firstDotPos < 0) {
			// There's no "dot" so look for the sublocation at this level.
			result = this.findLocationById(inLocationId);
		} else {
			// There is a dot, so find the sublocation based on the first part and recursively ask it for the location from the second part.
			String firstPart = inLocationId.substring(0, firstDotPos);
			String secondPart = inLocationId.substring(firstDotPos + 1);
			ISubLocation<?> firstPartLocation = this.findLocationById(firstPart);
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
			ILocation<?> parent = (ILocation<?>) getParent();
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
			return theAlias.getAlias();
		}
		return "";
	}

	public final void addVertex(Vertex inVertex) {
		vertices.add(inVertex);
	}

	public final void removeVertex(Vertex inVertex) {
		vertices.remove(inVertex);
	}

	public final void addAlias(LocationAlias inAlias) {
		aliases.add(inAlias);
	}

	public final void removeAlias(LocationAlias inAlias) {
		aliases.remove(inAlias);
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
		String domainId = Item.makeDomainId(inItem.getItemId(), this, inItem.getUomMasterId());
		// why not just ask the item for its domainId?
		storedItems.put(domainId, inItem);
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
		ISubLocation<?> location = this.findSubLocationById(inLocationName);
		if (location != null)
			returnItem = location.getStoredItemFromMasterIdAndUom(inItemMasterId, inUom);
		return returnItem;
	}

	public final Item getStoredItem(final String inItemDomainId) {
		return storedItems.get(inItemDomainId);
	}

	public final void removeStoredItemFromMasterIdAndUom(final String inItemMasterId, final String inUom) {
		String domainId = Item.makeDomainId(inItemMasterId, this, inUom);
		storedItems.remove(domainId);
	}

	public final void removeStoredItem(final String inItemDomainId) {
		storedItems.remove(inItemDomainId);
	}

	public final void addItemDdcGroup(ItemDdcGroup inItemDdcGroup) {
		itemDdcGroups.put(inItemDdcGroup.getDdcGroupId(), inItemDdcGroup);
	}

	public final ItemDdcGroup getItemDdcGroup(final String inItemDdcGroupId) {
		return itemDdcGroups.get(inItemDdcGroupId);
	}

	public final void removeItemDdcGroup(final String inItemDdcGroupId) {
		itemDdcGroups.remove(inItemDdcGroupId);
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
	@SuppressWarnings("rawtypes")
	public static class LocationWorkingOrderComparator implements Comparator<ILocation> {

		public int compare(ILocation inLoc1, ILocation inLoc2) {
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

	public List<ILocation<?>> getSubLocationsInWorkingOrder() {
		List<ILocation<?>> result = new ArrayList<ILocation<?>>();
		@SuppressWarnings("rawtypes")
		List<ISubLocation> childLocations = getActiveChildren();
		Collections.sort(childLocations, new LocationWorkingOrderComparator());
		for (ILocation<?> childLocation : childLocations) {
			// add sublocation
			result.add(childLocation);
			// and its sublocations recursively
			result.addAll(childLocation.getSubLocationsInWorkingOrder());
		}
		return result;
	}
	
	@Override
	public List<ISubLocation> getChildrenInWorkingOrder() {
		@SuppressWarnings("rawtypes")
		List<ISubLocation> childLocations = getActiveChildren();
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
			ILocation<?> aLocation = (ILocation<?>) this.getParent();
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
			ILocation<?> aLocation = (ILocation<?>) this.getParent();
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
			for (ISubLocation<?> child : getActiveChildren()) {
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

	public LedRange getFirstLastLedsForLocation() {
		// This often returns the stated leds for slots. But if the span is large, returns the central 4 leds.
		// to compute, we need the locations first and last led positions
		int firstLocLed = getFirstLedNumAlongPath();
		int lastLocLed = getLastLedNumAlongPath();
		// following cast not safe if the stored location is facility
		if (this instanceof Facility)
			return LedRange.zero(); // was initialized to give values of 0,0

		boolean lowerLedNearAnchor = this.isLowerLedNearAnchor();

		LedRange theLedRange = LedRange.computeLedsToLightForLocationNoOffset(firstLocLed, lastLocLed, lowerLedNearAnchor);

		return theLedRange;
	}

	private class InventoryPositionComparator implements Comparator<Item> {

		public int compare(Item item1, Item item2) {
			Double item1Pos = item1.getPosAlongPath();
			Double item2Pos = item2.getPosAlongPath();

			if (item1Pos == null && item2Pos == null)
				return 0;
			else if (item1Pos == null)
				return 1;
			else if (item2Pos == null)
				return -1;

			if (item1Pos > item2Pos)
				return -1;
			else if (item1Pos < item2Pos)
				return 1;
			return 0;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Gets inventory for this instance and all of its descendents then sorts along path. 
	 * Public mainly for testability
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Item> getInventoryInWorkingOrder() {
		ArrayList<Item> aList = Lists.newArrayList();
		// Add my inventory
		aList.addAll(getStoredItems().values());
		// Add my children's inventory
		for (SubLocationABC location : locations.values()) {
			aList.addAll(location.getInventoryInWorkingOrder());
		}
		// Sort as we want it
		Collections.sort(aList, new InventoryPositionComparator());

		return aList;
	}

}
