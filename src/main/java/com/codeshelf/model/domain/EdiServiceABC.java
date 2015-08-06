/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceABC.java,v 1.21 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;

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
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.EdiExportAccumulator;
import com.codeshelf.edi.WiBeanStringifier;
import com.codeshelf.edi.WorkInstructionCsvBean;
import com.codeshelf.model.EdiProviderEnum;
import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.service.ExtensionPointService;
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
@Table(name = "edi_service", uniqueConstraints = { @UniqueConstraint(columnNames = { "parent_persistentid", "domainid" }),
		@UniqueConstraint(columnNames = { "parent_persistentid", "provider" }),
		@UniqueConstraint(columnNames = { "parent_persistentid", "dtype" }) })
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class EdiServiceABC extends DomainObjectTreeABC<Facility> implements IEdiService {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiServiceABC.class);

	public static class EdiServiceABCDao extends GenericDaoABC<EdiServiceABC> implements ITypedDao<EdiServiceABC> {
		public final Class<EdiServiceABC> getDaoClass() {
			return EdiServiceABC.class;
		}
	}

	// The owning Facility.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private Facility				parent;

	// The provider.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
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

	// The credentials (encoded toekns or obfuscated keys only).
	@Column(nullable = true, name = "provider_credentials")
	@Getter
	@Setter
	@JsonProperty
	private String					providerCredentials;

	@Getter
	@Setter
	@Transient
	private EdiExportAccumulator	ediExportAccumulator;

	@Getter
	@Transient
	private ExtensionPointService	extensionPointService;

	public EdiServiceABC() {
		if (needsEdiOutputAccumulator())
			createEdiOutputAccumulator();

		if (needsGroovyOutputExtensions()) {
			// initialize scripting service
			try {
				extensionPointService = ExtensionPointService.createInstance(getFacility());
			} catch (Exception e) {
				LOGGER.error("Failed to initialize extension point service", e);
			}
		}

	}

	/**
	 * Override this to use the standard output accumulator on your EDI service.
	 * If the standard accumulator is not suitable, also override createEdiOutputAccumulator()
	 */
	protected boolean needsEdiOutputAccumulator() {
		return false;
	}

	/**
	 * Override this to use the standard groovy WI transformation (and future ones) on your EDI service that includes output.
	 */
	protected boolean needsGroovyOutputExtensions() {
		return false;
	}

	/**
	 * Override this to use the standard output accumulator on your EDI service.
	 * If the standard accumulator is not suitable, also override createEdiOutputAccumulator()
	 */
	protected boolean hasEdiOutputAccumulator() {
		return ediExportAccumulator != null;
	}

	protected void createEdiOutputAccumulator() {
		ediExportAccumulator = new EdiExportAccumulator();
	}

	public Facility getFacility() {
		return getParent();
	}

	public final String getDefaultDomainIdPrefix() {
		return "EDI";
	}

	@JsonProperty
	public abstract boolean getHasCredentials();

	/**
	 * Add this work instruction accumulation list to send to host.
	 * This is initially tailored to PFSWeb data interchange, mimicking Dematic cart
	 */
	public void notifyWiComplete(WorkInstruction inWi) {
		if (hasEdiOutputAccumulator())
			getEdiExportAccumulator().addWorkInstruction(inWi);
	}

	/**
	 * If this host needs it, tell about cart setup
	 * This is initially tailored to PFSWeb data interchange, mimicking Dematic cart
	 */
	public void notifyOrderOnCart(OrderHeader inOrder, Che inChe) {

	}

	/**
	 * If this host needs it this way, send off the order with accumulated work instructions
	 * This is initially tailored to PFSWeb data interchange, mimicking Dematic cart
	 */
	public void notifyOrderCompleteOnCart(OrderHeader inOrder, Che inChe) {
		if (!hasEdiOutputAccumulator())
			return;
		ArrayList<WorkInstructionCsvBean> beanList = getEdiExportAccumulator().getAndRemoveWiBeansFor(inOrder.getOrderId(),
			inChe.getDomainId());
		// This list has "complete" work instruction beans. The particular customer's EDI may need strange handling.
		processOrderCompleteOnCart(inOrder, inChe, beanList);
	}

	/**
	 * This returns a string that will be fed into some transport means.
	 * For PFSWeb, string -> file -> ftp service
	 * The standard behavior is the bean export with header. Groovy extensions modify the header, trailer, bean content, and line format.
	 */
	protected String processOrderCompleteOnCart(OrderHeader inOrder, Che inChe, ArrayList<WorkInstructionCsvBean> wiBeanList) {
		
		WiBeanStringifier stringifier = new WiBeanStringifier(inOrder, inChe, wiBeanList,getExtensionPointService() );
		
		return stringifier.stringify();
		
	}

	/**
	 * If this host needs it this way, send off the order with accumulated work instructions
	 * Not sure PFSWeb can tolerate this. They owe an answer what such a message should look like.
	 * This may or may not include completed work instructions for that order.
	 */
	public void notifyOrderRemoveFromCart(OrderHeader inOrder, Che inChe) {

	}

}
