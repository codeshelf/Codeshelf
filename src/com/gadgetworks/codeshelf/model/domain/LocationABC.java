/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationABC.java,v 1.13 2012/10/30 15:21:34 jeffw Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

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
@Table(name = "LOCATION")
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public abstract class LocationABC<P extends IDomainObject> extends DomainObjectTreeABC<P> {

	@Inject
	public static ITypedDao<LocationABC>	DAO;

	@Singleton
	public static class LocationDao extends GenericDaoABC<LocationABC> implements ITypedDao<LocationABC> {
		public final Class<LocationABC> getDaoClass() {
			return LocationABC.class;
		}
	}

	private static final Log			LOGGER		= LogFactory.getLog(LocationABC.class);

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

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = true)
	private LocationABC<P>				parent;

	// All of the vertices that define the location's footprint.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<Vertex>				vertices	= new ArrayList<Vertex>();

	// The child locations.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, LocationABC>	locations	= new HashMap<String, LocationABC>();

	// The items stored in this location.
	@OneToMany(mappedBy = "parent")
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

	public final P getParent() {
		return (P) parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof LocationABC) {
			parent = (LocationABC) inParent;
		}
	}

	public final List<LocationABC> getChildren() {
		return new ArrayList<LocationABC>(locations.values());
	}

	public final void addLocation(LocationABC inLocation) {
		locations.put(inLocation.getDomainId(), inLocation);
	}

	public final LocationABC getLocation(String inLocationId) {
		return locations.get(inLocationId);
	}

	public final void removeLocation(String inLocationId) {
		locations.remove(inLocationId);
	}

	public final LocationABC getLocationById(final String inLocationId) {
		LocationABC result = null;

		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("theId", inLocationId);
		List<LocationABC> foundLocations = getDao().findByFilterAndClass("domainId = :theId", filterParams, LocationABC.class);
		if ((foundLocations != null) && (foundLocations.size() > 0) && (foundLocations.get(0) instanceof LocationABC)) {
			result = (LocationABC) foundLocations.get(0);
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
