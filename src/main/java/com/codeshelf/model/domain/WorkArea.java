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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

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
 * WorkArea
 * 
 * A collection of locations where a worker (user) executes work instructions.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "work_area", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class WorkArea extends DomainObjectTreeABC<Path> {

	public static class WorkAreaDao extends GenericDaoABC<WorkArea> implements ITypedDao<WorkArea> {
		public final Class<WorkArea> getDaoClass() {
			return WorkArea.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER		= LoggerFactory.getLogger(WorkArea.class);

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
		return staticGetDao();
	}

	public static ITypedDao<WorkArea> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(WorkArea.class);
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

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}
}
