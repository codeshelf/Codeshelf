/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

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
 * LocationAlias
 * 
 * Map a string to a fully qualified location name in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "location_alias")
@CacheStrategy(useBeanCache = true)@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString(of = { "mappedLocation", "active" }, callSuper = true, doNotUseGetters = true)
public class LocationAlias extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<LocationAlias>	DAO;

	@Singleton
	public static class LocationAliasDao extends GenericDaoABC<LocationAlias> implements ITypedDao<LocationAlias> {
		@Inject
		public LocationAliasDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<LocationAlias> getDaoClass() {
			return LocationAlias.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAlias.class);

	// Attachment credential.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonProperty
	private LocationABC			mappedLocation;

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
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility			parent;

	public LocationAlias() {

	}

	public final ITypedDao<LocationAlias> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "MAP";
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final void setLocationAlias(String inLocationAlias) {
		setDomainId(inLocationAlias);
	}

	public final String getAlias() {
		return getDomainId();
	}

	public final ISubLocation<?> getMappedLocation() {
		return (ISubLocation<?>) mappedLocation;
	}

	public final void setMappedLocation(final ILocation<?> inMappedLocation) {
		mappedLocation = (LocationABC) inMappedLocation;
	}

}
