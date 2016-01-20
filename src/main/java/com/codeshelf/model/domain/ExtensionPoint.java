/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2015, All rights reserved
 *******************************************************************************/
package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.ExtensionPointType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "extension_point", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ExtensionPoint extends DomainObjectTreeABC<Facility> {

	public static class ExtensionPointDao extends GenericDaoABC<ExtensionPoint> implements ITypedDao<ExtensionPoint> {
		public final Class<ExtensionPoint> getDaoClass() {
			return ExtensionPoint.class;
		}
	}

	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private boolean	active=true;

	// The extension point this script instance implements.
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	ExtensionPointType type;
	
	@Column(nullable = false, columnDefinition = "TEXT")
	@Getter @Setter
	@JsonProperty
	String script;

	public ExtensionPoint() {
		super();
	}

	public ExtensionPoint(Facility facility, ExtensionPointType type) {
		super();
		setParent(facility);
		setDomainId(facility.getDomainId() + "-" + type.name());
		setType(type);
		setActive(false);
		setScript(type.getExampleScript());
	}

	
	@SuppressWarnings("unchecked")
	public final ITypedDao<ExtensionPoint> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<ExtensionPoint> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ExtensionPoint.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "EXP";
	}

	public Facility getFacility() {
		return this.getParent();
	}	
}
