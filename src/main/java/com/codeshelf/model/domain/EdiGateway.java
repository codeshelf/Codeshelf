/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceABC.java,v 1.21 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.edi.IEdiGateway;
import com.codeshelf.model.EdiProviderEnum;
import com.codeshelf.model.EdiServiceStateEnum;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@Table(name = "edi_service", uniqueConstraints = { 
		@UniqueConstraint(columnNames = { "parent_persistentid", "domainid" }),
		@UniqueConstraint(columnNames = { "parent_persistentid", "dtype" }) })
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class EdiGateway extends DomainObjectTreeABC<Facility> implements IEdiGateway {

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private Facility				parent;

	// The provider.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Setter
	@JsonProperty
	private EdiProviderEnum			provider;

	// Service state.
	@Column(nullable = false, name = "service_state")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private EdiServiceStateEnum		serviceState;

	// Access tokens and any other configuration to be interpreted by subclass.
	// If the service requires storing a password, it should be obfuscated. 
	@Column(nullable = true, name = "provider_credentials")
	@Getter
	@Setter
	@JsonProperty
	private String					providerCredentials;
	
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Boolean					active;
	

	public EdiGateway() {

	}

	public boolean isLinked() {
		return EdiServiceStateEnum.LINKED.equals(this.getServiceState());
	}

	public Facility getFacility() {
		return getParent();
	}

	public final String getDefaultDomainIdPrefix() {
		return "EDI";
	}

	@JsonProperty
	public abstract boolean getHasCredentials();


}
