/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceABC.java,v 1.21 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.EdiProviderEnum;
import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * EDI Service
 * 
 * An EDI service allows the system to import/export EDI documentLocators to/from other systems.
 * 
 * @author jeffw
 */

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "edi_service")
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class EdiServiceABC extends DomainObjectTreeABC<Facility> implements IEdiService {

	private static final Logger			LOGGER				= LoggerFactory.getLogger(EdiServiceABC.class);

	@Inject
	public static ITypedDao<EdiServiceABC>	DAO;

	
	@Singleton
	public static class EdiServiceABCDao
		extends GenericDaoABC<EdiServiceABC> implements ITypedDao<EdiServiceABC> {
		@Inject
		public EdiServiceABCDao(TenantPersistenceService tenantPersistenceService) {
			super(tenantPersistenceService);
		}

		public final Class<EdiServiceABC> getDaoClass() {
			return EdiServiceABC.class;
		}
	}
	 
	
	// The owning Facility.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Facility					parent;

	// The provider.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private EdiProviderEnum				provider;

	// Service state.
	@Column(nullable = false,name="service_state")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private EdiServiceStateEnum			serviceState;

	// The credentials (encoded toekns or obfuscated keys only).
	@Column(nullable = true,name="provider_credentials")
	@Getter
	@Setter
	@JsonProperty
	private String						providerCredentials;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<EdiDocumentLocator>	documentLocators	= new ArrayList<EdiDocumentLocator>();

	public EdiServiceABC() {

	}
	
	public Facility getFacility() {
		return getParent();
	}


	public final String getDefaultDomainIdPrefix() {
		return "EDI";
	}

	@JsonProperty
	public abstract boolean getHasCredentials();
	
	public void addEdiDocumentLocator(EdiDocumentLocator inEdiDocumentLocator) {
		EdiServiceABC previousEdiService = inEdiDocumentLocator.getParent();
		if(previousEdiService == null) {
			documentLocators.add(inEdiDocumentLocator);
			inEdiDocumentLocator.setParent(this);
		} else if(!previousEdiService.equals(this)) {
			LOGGER.error("cannot add EdiDocumentLocator "+inEdiDocumentLocator.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousEdiService.getDomainId());
		}	
	}

	public void removeEdiDocumentLocator(EdiDocumentLocator inEdiDocumentLocator) {
		if(this.documentLocators.contains(inEdiDocumentLocator)) {
			inEdiDocumentLocator.setParent(null);
			documentLocators.remove(inEdiDocumentLocator);
		} else {
			LOGGER.error("cannot remove EdiDocumentLocator "+inEdiDocumentLocator.getDomainId()+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}
}
