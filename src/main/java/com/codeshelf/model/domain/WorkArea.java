/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkArea.java,v 1.17 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * WorkArea
 * 
 * A collection of locations where a worker (user) executes work instructions.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "work_area")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class WorkArea extends DomainObjectTreeABC<Path> {

	@Inject
	public static ITypedDao<WorkArea>	DAO;

	@Singleton
	public static class WorkAreaDao extends GenericDaoABC<WorkArea> implements ITypedDao<WorkArea> {
		@Inject
		public WorkAreaDao(final TenantPersistenceService tenantPersistenceService) {
			super(tenantPersistenceService);
		}
		
		public final Class<WorkArea> getDaoClass() {
			return WorkArea.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER		= LoggerFactory.getLogger(WorkArea.class);

	// The parent facility.
	@OneToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Path parent;

	// The work area ID.
	@Column(nullable = false,name="work_area_id")
	@Getter
	@Setter
	@JsonProperty
	private String				workAreaId;

	// The work description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				description;

	// A work area is a collection of locations.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<Location>	locations	= new ArrayList<Location>();

	public WorkArea() {
		workAreaId = "";
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<WorkArea> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public void addLocation(Location inSubLocation) {
		locations.add(inSubLocation);
	}

	public void removeLocation(Location inLocation) {
		locations.remove(inLocation);
	}

	public static void setDao(WorkAreaDao inWorkAreaDao) {
		WorkArea.DAO = inWorkAreaDao;
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}
}
