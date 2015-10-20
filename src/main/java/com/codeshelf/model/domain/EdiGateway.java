/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiGateway.java,v 1.21 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.UUID;

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
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.EdiGatewayStateEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
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
	private static final String	TIME_FORMAT	= "HH-mm-ss-SSS";

	private static HashMap<UUID, Timestamp> lastSuccessTimes = new HashMap<>();
	
	public static class EdiGatewayDao extends GenericDaoABC<EdiGateway> implements ITypedDao<EdiGateway> {
		public final Class<EdiGateway> getDaoClass() {
			return EdiGateway.class;
		}
	}

	public static ITypedDao<EdiGateway> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(EdiGateway.class);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<EdiGateway> getDao() {
		return staticGetDao();
	}

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private Facility				parent;

	// The provider.
	@Column(nullable = false, name = "transport_type")
	@Enumerated(value = EnumType.STRING)
	@Setter
	@JsonProperty
	private EdiTransportType		transportType;

	// Service state.
	@Column(nullable = false, name = "service_state")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private EdiGatewayStateEnum		gatewayState;

	// Access tokens and any other configuration to be interpreted by subclass.
	// If the service requires storing a password, it should be obfuscated. 
	@Column(nullable = true, name = "provider_credentials")
	@Getter
	@Setter
	@JsonProperty
	private String					providerCredentials;
	
	@Column(nullable = true)
	@Setter
	@JsonProperty
	private Boolean					active;

	public EdiGateway() {

	}

	public boolean isLinked() {
		return EdiGatewayStateEnum.LINKED.equals(this.getGatewayState());
	}

	public Facility getFacility() {
		return getParent();
	}

	public final String getDefaultDomainIdPrefix() {
		return "EDI";
	}

	@JsonProperty
	public abstract boolean getHasCredentials();

	@Override
	public boolean isActive() {
		if (active == null){
			return false;
		}
		return active;
	}
	
	public synchronized Timestamp getLastSuccessTime(){
		Timestamp time = lastSuccessTimes.get(getPersistentId());
		return time == null ? new Timestamp(0) : time;
	}
	
	public synchronized void updateLastSuccessTime(){
		lastSuccessTimes.put(getPersistentId(), new Timestamp(System.currentTimeMillis()));
	}

	protected String safeTimestamp(long time) {
		return new SimpleDateFormat(TIME_FORMAT).format(time);
	}
}
