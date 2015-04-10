/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

// --------------------------------------------------------------------------
/**
 * LocationAlias
 *
 * Map a string to a fully qualified location name in the facility.
 *
 * @author jeffw
 */

@Entity
@Table(name = "location_alias")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "mappedLocation", "active" }, callSuper = true, doNotUseGetters = true)
public class LocationAlias extends DomainObjectTreeABC<Facility> {

	public static class LocationAliasDao extends GenericDaoABC<LocationAlias> implements ITypedDao<LocationAlias> {
		public final Class<LocationAlias> getDaoClass() {
			return LocationAlias.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAlias.class);

	// Attachment credential.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "mapped_location_persistentid")
	@JsonProperty
	@Getter
	@Setter
	private Location			mappedLocation;

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

	// The owning facility.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private Facility			parent;

	public LocationAlias() {
	}

	public LocationAlias(Facility facility, String domainId, Location mappedLocation) {
		super(domainId);
		this.parent = facility;
		this.mappedLocation = mappedLocation;
		this.parent = facility;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<LocationAlias> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<LocationAlias> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(LocationAlias.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "MAP";
	}

	public Facility getFacility() {
		return getParent();
	}

	public void setLocationAlias(String inLocationAlias) {
		setDomainId(inLocationAlias);
	}

	public String getAlias() {
		return getDomainId();
	}

	public String getNominalLocationId() {
		Location theLocation = getMappedLocation();
		if (theLocation != null)
			return theLocation.getNominalLocationId();
			// if the location is inactive, has brackets around it.
		else
			return "";
	}

}
