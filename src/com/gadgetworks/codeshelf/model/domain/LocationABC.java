/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationABC.java,v 1.21 2013/03/07 05:23:32 jeffw Exp $
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
import javax.persistence.OneToOne;
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
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
@CacheStrategy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "LOCATION", schema = "CODESHELF")
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString
public abstract class LocationABC<P extends IDomainObject> extends DomainObjectTreeABC<P> {

	@Inject
	public static ITypedDao<LocationABC>	DAO;

	@Singleton
	public static class LocationDao extends GenericDaoABC<LocationABC> implements ITypedDao<LocationABC> {
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
	private PositionTypeEnum			posType;

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

	// Associated path segment (optional)
	@Column(nullable = true)
	@OneToOne(optional = true)
	@Getter
	@Setter
	private PathSegment					pathSegment;

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private Organization				parentOrganization;

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
	private Map<String, Item>			items		= new HashMap<String, Item>();

	public LocationABC() {

	}

	public LocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY) {
		posType = inPosType;
		posX = inPosX;
		posY = inPosY;
		// Z pos is non-null so that it doesn't need to be explicitly set.
		posZ = 0.0;
	}

	public LocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY, final double inPosZ) {
		posType = inPosType;
		posX = inPosX;
		posY = inPosY;
		posZ = inPosZ;
	}

	public final List<SubLocationABC> getChildren() {
		return new ArrayList<SubLocationABC>(locations.values());
	}

	// --------------------------------------------------------------------------
	/**
	 * Get all of the children of this type (no matter how far down the hierarchy).
	 * 
	 * To get it to strongly type the return for you then use this unusual Java construct at the caller:
	 * 
	 * Aisle aisle = facility.<Aisle> getChildrenAtLevel(Aisle.class)
	 * (If calling this method from a generic location type then you need to define it as LocationABC<?> location.)
	 * 
	 * @param inClassWanted
	 * @return
	 */
	public final <T extends LocationABC> List<T> getChildrenAtLevel(Class<? extends LocationABC> inClassWanted) {
		List<T> result = new ArrayList<T>();

		// Loop through all of the children.
		for (LocationABC<P> child : getChildren()) {
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
	public final <T extends LocationABC> T getParentAtLevel(Class<? extends LocationABC> inClassWanted) {
		T result = null;

		LocationABC<P> parent = (LocationABC) getParent();

		if (parent.getClass().equals(inClassWanted)) {
			// This is the parent we want. (We can cast safely since we checked the class.)
			result = (T) parent;
		} else {
			if (parent.getClass().equals(Facility.class)) {
				// We cannot go higher than the Facility as a parent, so there is no such parent with the requested class.
				result = null;
			} else {
				// The current parent is not the class we want so recurse up the hierarchy.
				result = parent.getParentAtLevel(inClassWanted);
			}
		}

		return result;
	}

	public final String getLocationId() {
		return getDomainId();
	}

	public final void setLocationId(final String inLocationId) {
		setDomainId(inLocationId);
	}

	public final void addLocation(SubLocationABC inLocation) {
		locations.put(inLocation.getDomainId(), inLocation);
	}

	public final SubLocationABC<P> getLocation(String inLocationId) {
		return locations.get(inLocationId);
	}

	public final void removeLocation(String inLocationId) {
		locations.remove(inLocationId);
	}

	// --------------------------------------------------------------------------
	/**
	 * Look for any sub-location by it's ID.
	 * The location ID needs to be a dotted notation where the first octet is a child location of "this" location.
	 * @param inLocationId
	 * @return
	 */
	public final LocationABC<P> getSubLocationById(final String inLocationId) {
		LocationABC<P> result = null;

		Integer firstDotPos = inLocationId.indexOf(".");
		if (firstDotPos < 0) {
			// There's no "dot" so look for the sublocation at this level.
			result = this.getLocation(inLocationId);
		} else {
			// There is a dot, so find the sublocation based on the first part and recursively ask it for the location from the second part.
			String firstPart = inLocationId.substring(0, firstDotPos);
			String secondPart = inLocationId.substring(firstDotPos + 1);
			LocationABC<P> subLocation = this.getLocation(firstPart);
			if (subLocation != null) {
				result = subLocation.getSubLocationById(secondPart);
			}
		}
		return result;
	}

	public final void setPosTypeByStr(String inPosTypeStr) {
		setPosType(PositionTypeEnum.valueOf(inPosTypeStr));
	}

	public final void addVertex(Vertex inVertex) {
		vertices.add(inVertex);
	}

	public final void removeVertex(Vertex inVertex) {
		vertices.remove(inVertex);
	}

	public final void addItem(final String inItemId, Item inItem) {
		items.put(inItemId, inItem);
	}

	public final Item getItem(final String inItemId) {
		return items.get(inItemId);
	}

	public final void removeItem(final String inItemId) {
		items.remove(inItemId);
	}
}
