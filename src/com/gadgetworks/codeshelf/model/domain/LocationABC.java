/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationABC.java,v 1.40 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class LocationABC<P extends IDomainObject> extends DomainObjectTreeABC<P> implements ILocation<P> {

	@Inject
	public static ITypedDao<LocationABC>	DAO;

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
	public static final Double			METERS_PER_LED_POS	= 0.03333;

	private static final Logger			LOGGER				= LoggerFactory.getLogger(LocationABC.class);

	// The position type (GPS, METERS, etc.).
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PositionTypeEnum			posTypeEnum;

	// The X anchor position.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double						posX;

	// The Y anchor position.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double						posY;

	// The Z anchor position.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	// Null means it's at the same nominal z coord as the parent.
	private Double						posZ;

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
	//	@Setter
	private PathSegment					pathSegment;

	// The owning organization.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private Organization				parentOrganization;

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
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, SubLocationABC>	locations			= new HashMap<String, SubLocationABC>();

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

	public LocationABC() {

	}

	public LocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY) {
		posTypeEnum = inPosType;
		posX = inPosX;
		posY = inPosY;
		// Z pos is non-null so that it doesn't need to be explicitly set.
		posZ = 0.0;
	}

	public LocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY, final double inPosZ) {
		posTypeEnum = inPosType;
		posX = inPosX;
		posY = inPosY;
		posZ = inPosZ;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getChildren()
	 */
	public final List<ISubLocation> getChildren() {
		return new ArrayList<ISubLocation>(locations.values());
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getChildrenAtLevel(java.lang.Class)
	 */
	public final <T extends ISubLocation> List<T> getChildrenAtLevel(Class<? extends ISubLocation> inClassWanted) {
		List<T> result = new ArrayList<T>();

		// Loop through all of the children.
		for (ILocation<P> child : getChildren()) {
			if (child.getClass().equals(inClassWanted)) {
				// If the child is the kind we want then add it to the list.
				result.add((T) child);
			} else {
				// If the child is not the kind we want the recurse.
				result.addAll((List<T>) child.getChildrenAtLevel(inClassWanted));
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
	public final String getLocationIdToParentLevel(Class<? extends ILocation> inClassWanted) {
		String result;

		ILocation<P> checkParent = (ILocation<P>) getParent();

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
				result = result + "." + checkParent.getLocationId();
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getParentAtLevel(java.lang.Class)
	 */
	public final <T extends ILocation> T getParentAtLevel(Class<? extends ILocation> inClassWanted) {
		T result = null;

		ILocation<P> checkParent = (ILocation<P>) getParent();

		// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
		// This fixes that problem, but it's not pretty.
		checkParent = DAO.findByPersistentId(checkParent.getClass(), checkParent.getPersistentId());

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

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.ILocation#getAbsolutePosX()
	 */
	public Double getAbsolutePosX() {
		Double result = getPosX();

		if (!posTypeEnum.equals(PositionTypeEnum.GPS)) {
			ILocation<P> parent = (ILocation<P>) getParent();

			// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
			// This fixes that problem, but it's not pretty.
			parent = DAO.findByPersistentId(parent.getClass(), parent.getPersistentId());
			if ((parent != null) && (parent.getPosTypeEnum().equals(PositionTypeEnum.METERS_FROM_PARENT))) {
				result += parent.getAbsolutePosX();
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.ILocation#getAbsolutePosX()
	 */
	public Double getAbsolutePosY() {
		Double result = getPosY();

		if (!posTypeEnum.equals(PositionTypeEnum.GPS)) {
			ILocation<P> parent = (ILocation<P>) getParent();

			// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
			// This fixes that problem, but it's not pretty.
			parent = DAO.findByPersistentId(parent.getClass(), parent.getPersistentId());
			if ((parent != null) && (parent.getPosTypeEnum().equals(PositionTypeEnum.METERS_FROM_PARENT))) {
				result += parent.getAbsolutePosY();
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.ILocation#getAbsolutePosX()
	 */
	public Double getAbsolutePosZ() {
		Double result = getPosZ();

		if (!posTypeEnum.equals(PositionTypeEnum.GPS)) {
			ILocation<P> parent = (ILocation<P>) getParent();

			// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
			// This fixes that problem, but it's not pretty.
			parent = DAO.findByPersistentId(parent.getClass(), parent.getPersistentId());
			if ((parent != null) && (parent.getPosTypeEnum().equals(PositionTypeEnum.METERS_FROM_PARENT))) {
				result += parent.getAbsolutePosZ();
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
	public final void addLocation(ISubLocation inLocation) {
		// Ebean can't deal with interfaces.
		SubLocationABC<P> subLocation = (SubLocationABC<P>) inLocation;
		locations.put(inLocation.getDomainId(), subLocation);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getLocation(java.lang.String)
	 */
	public final ISubLocation findLocationById(String inLocationId) {
		// There's some ebean weirdness around Map caches, so we have to use a different strategy to resolve this request.
		//return locations.get(inLocationId);
		ISubLocation result = null;

		// If the current location is a facility then first look for an alias 
		if (this.getClass().equals(Facility.class)) {
			Facility facility = (Facility) this;
			LocationAlias alias = facility.getLocationAlias(inLocationId);
			if ((alias != null) && (alias.getActive())) {
				return alias.getMappedLocation();
			}
		}

		// We didn't find an alias, so search through DB for the matching location.
		ITypedDao<SubLocationABC> dao = SubLocationABC.DAO;
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("persistentId", this.getPersistentId().toString());
		filterParams.put("domainId", inLocationId);
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
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getSubLocationById(java.lang.String)
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
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#setPathSegment(com.gadgetworks.codeshelf.model.domain.PathSegment)
	 */
	public final void setPathSegment(final PathSegment inPathSegment) {

		// Set the path segment recursively for all of the child locations as well.
		for (ILocation<P> location : getChildren()) {
			location.setPathSegment(inPathSegment);
		}

		pathSegment = inPathSegment;
		//		try {
		//			LocationABC.DAO.store(this);
		//		} catch (DaoException e) {
		//			LOGGER.error("", e);
		//		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#computePathDistance()
	 */
	public final void computePathDistance() {

		// Also force a recompute for all of the child locations.
		for (ILocation<P> location : getChildren()) {
			location.computePathDistance();
		}

		// Now compute the path position for this location.
		Double pathPosition = 0.0;
		PathSegment segment = this.getPathSegment();
		if (segment != null) {
			ILocation<P> anchorLocation = segment.getAnchorLocation();
			Point locationPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, Math.abs(anchorLocation.getAbsolutePosX()
					+ this.getAbsolutePosX()), Math.abs(anchorLocation.getAbsolutePosY() + this.getAbsolutePosY()), null);
			if (segment.getParent().getTravelDirEnum().equals(TravelDirectionEnum.FORWARD)) {
				pathPosition = segment.getStartPosAlongPath()
						+ segment.computeDistanceOfPointFromLine(segment.getStartPoint(), segment.getEndPoint(), locationPoint);
			} else {
				pathPosition = segment.getStartPosAlongPath()
						+ segment.computeDistanceOfPointFromLine(segment.getEndPoint(), segment.getStartPoint(), locationPoint);
			}

			LOGGER.debug(this.getFullDomainId() + "Path pos: " + pathPosition + " Location Point - x: " + locationPoint.getX()
					+ " y: " + locationPoint.getY());
		}
		posAlongPath = pathPosition;

		try {
			LocationABC.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	public final void setPosTypeByStr(String inPosTypeStr) {
		setPosTypeEnum(PositionTypeEnum.valueOf(inPosTypeStr));
	}

	public final void addVertex(Vertex inVertex) {
		vertices.add(inVertex);
	}

	public final void removeVertex(Vertex inVertex) {
		vertices.remove(inVertex);
	}

	public final void addStoredItem(Item inItem) {
		String domainId = Item.makeDomainId(inItem.getItemId(), this);
		storedItems.put(domainId, inItem);
	}

	public final Item getStoredItem(final String inItemId) {
		String domainId = Item.makeDomainId(inItemId, this);
		return storedItems.get(domainId);
	}

	public final void removeStoredItem(final String inItemId) {
		String domainId = Item.makeDomainId(inItemId, this);
		storedItems.remove(domainId);
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

}
