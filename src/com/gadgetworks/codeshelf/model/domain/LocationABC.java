/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationABC.java,v 1.33 2013/04/11 18:11:12 jeffw Exp $
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

// --------------------------------------------------------------------------
/**
 * LocationABC
 * 
 * The anchor point and vertex collection to define the planar space of a work structure (e.g. facility, bay, shelf, etc.)
 * 
 * @author jeffw
 */

@Entity
@MappedSuperclass
@CacheStrategy(useBeanCache = false)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "location", schema = "codeshelf")
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class LocationABC<P extends IDomainObject> extends DomainObjectTreeABC<P> implements ILocation<P> {

	@Inject
	public static ITypedDao<LocationABC>	DAO;

//	@Singleton
	public static class LocationDao extends GenericDaoABC<LocationABC> implements ITypedDao<LocationABC> {
		@Inject
		public LocationDao(final ISchemaManager inSchemaManager, final IDatabase inDatabase) {
			super(inSchemaManager);
		}
		
		public final Class<LocationABC> getDaoClass() {
			return LocationABC.class;
		}
	}

	private static final Logger			LOGGER		= LoggerFactory.getLogger(LocationABC.class);

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
	private Double						pathDistance;

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
	private Integer						ledChannel;

	// The bay's first LED position on the channel.
	@Column(nullable = true)
	@Getter
	@Setter
	private Integer						firstLedPos;

	// The number of LED positions in the bay.
	@Column(nullable = true)
	@Getter
	@Setter
	private Integer						lastLedPos;

	// All of the vertices that define the location's footprint.
	@OneToMany(mappedBy = "parent")
	@Getter
	@Setter
	private List<Vertex>				vertices	= new ArrayList<Vertex>();

	// The child locations.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, SubLocationABC>	locations	= new HashMap<String, SubLocationABC>();

	// The items stored in this location.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@Setter
	private Map<String, Item>			items		= new HashMap<String, Item>();

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
				result.addAll((List<T>) getChildrenAtLevel(inClassWanted));
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

		ILocation<P> parent = (ILocation<P>) getParent();

		// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
		// This fixes that problem, but it's not pretty.
		parent = DAO.findByPersistentId(parent.getClass(), parent.getPersistentId());

		if (parent.getClass().equals(inClassWanted)) {
			// This is the parent we want. (We can cast safely since we checked the class.)
			result = (T) parent;
		} else {
			if (parent.getClass().equals(Facility.class)) {
				// We cannot go higher than the Facility as a parent, so there is no such parent with the requested class.
				result = null;
			} else {
				// The current parent is not the class we want so recurse up the hierarchy.
				result = (T) parent.getParentAtLevel(inClassWanted);
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
	public final ISubLocation getLocation(String inLocationId) {
		// There's some ebean weirdness around Map caches, so we have to use a different strategy to resolve this request.
		//return locations.get(inLocationId);
		ISubLocation result = null;

		ITypedDao<SubLocationABC> dao = SubLocationABC.DAO;
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("persistentId", this.getPersistentId().toString());
		filterParams.put("domainId", inLocationId);
		List<SubLocationABC> resultSet = dao.findByFilter("parent.persistentId = :persistentId and domainId = :domainId", filterParams);
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
	public final ILocation<P> getSubLocationById(final String inLocationId) {
		ILocation<P> result = null;

		Integer firstDotPos = inLocationId.indexOf(".");
		if (firstDotPos < 0) {
			// There's no "dot" so look for the sublocation at this level.
			result = this.getLocation(inLocationId);
		} else {
			// There is a dot, so find the sublocation based on the first part and recursively ask it for the location from the second part.
			String firstPart = inLocationId.substring(0, firstDotPos);
			String secondPart = inLocationId.substring(firstDotPos + 1);
			ILocation<P> subLocation = this.getLocation(firstPart);
			if (subLocation != null) {
				result = subLocation.getSubLocationById(secondPart);
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
		try {
			LocationABC.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
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

		// Now compute the distance for this location.
		Double distance = 0.0;
		PathSegment segment = this.getPathSegment();
		if (segment != null) {
			ILocation<P> anchorLocation = segment.getAnchorLocation();
			if (segment.getParent().getTravelDirEnum().equals(TravelDirectionEnum.FORWARD)) {
				Point locationPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorLocation.getPosX() + this.getPosX(), anchorLocation.getPosY() + this.getPosY(), null);
				distance = segment.getPathDistance() + segment.computeDistanceOfPointFromLine(segment.getStartPoint(), segment.getEndPoint(), locationPoint);
			} else {
				Point locationPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorLocation.getPosX() + this.getPosX(), anchorLocation.getPosY() + this.getPosY(), null);
				distance = segment.getPathDistance() + segment.computeDistanceOfPointFromLine(segment.getEndPoint(), segment.getStartPoint(), locationPoint);
			}
		}
		pathDistance = distance;

		try {
			LocationABC.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#setPosTypeByStr(java.lang.String)
	 */
	public final void setPosTypeByStr(String inPosTypeStr) {
		setPosTypeEnum(PositionTypeEnum.valueOf(inPosTypeStr));
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#addVertex(com.gadgetworks.codeshelf.model.domain.Vertex)
	 */
	public final void addVertex(Vertex inVertex) {
		vertices.add(inVertex);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#removeVertex(com.gadgetworks.codeshelf.model.domain.Vertex)
	 */
	public final void removeVertex(Vertex inVertex) {
		vertices.remove(inVertex);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#addItem(java.lang.String, com.gadgetworks.codeshelf.model.domain.Item)
	 */
	public final void addItem(final String inItemId, Item inItem) {
		items.put(inItemId, inItem);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#getItem(java.lang.String)
	 */
	public final Item getItem(final String inItemId) {
		return items.get(inItemId);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#removeItem(java.lang.String)
	 */
	public final void removeItem(final String inItemId) {
		items.remove(inItemId);
	}
}
