/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2015, All rights reserved
 *******************************************************************************/
package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.ExtensionPoint;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "script")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Script extends DomainObjectTreeABC<Facility> {

	public static class ScriptDao extends GenericDaoABC<Script> implements ITypedDao<Script> {
		public final Class<Script> getDaoClass() {
			return Script.class;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Script.class);

	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private boolean	active=true;

	// The owning facility.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter @Setter
	@JsonProperty
	private Facility parent;
	
	// The extension point this script instance implements.
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	ExtensionPoint extension;
	
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	String body;

	public Script() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<Script> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<Script> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Script.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "SCRIPT";
	}

	public Facility getFacility() {
		return this.getParent();
	}	
}
