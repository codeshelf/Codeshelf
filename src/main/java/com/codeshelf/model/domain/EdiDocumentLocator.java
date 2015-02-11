/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentLocator.java,v 1.22 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.codeshelf.model.EdiDocumentStatusEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Edi Document Locator
 * 
 * This holds relevant information for an EDI document held by the parent EDI service
 * 
 * @author jeffw
 */

@Entity
@Table(name = "edi_document_locator")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class EdiDocumentLocator extends DomainObjectTreeABC<EdiServiceABC> {

	@Inject
	public static ITypedDao<EdiDocumentLocator>	DAO;

	@Singleton
	public static class EdiDocumentLocatorDao extends GenericDaoABC<EdiDocumentLocator> implements ITypedDao<EdiDocumentLocator> {
		@Inject
		public EdiDocumentLocatorDao(TenantPersistenceService tenantPersistenceService) {
			super(tenantPersistenceService);
		}
		
		public final Class<EdiDocumentLocator> getDaoClass() {
			return EdiDocumentLocator.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger		LOGGER	= LoggerFactory.getLogger(EdiDocumentLocator.class);

	// The owning EdiService.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private EdiServiceABC			parent;

	// Document Path
	@Column(nullable = false,name="document_path")
	@Getter
	@Setter
	@JsonProperty
	private String					documentPath;

	// Document Name
	@Column(nullable = true,name="document_name")
	@Getter
	@Setter
	@JsonProperty
	private String					documentName;

	// Received date.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp				received;

	// Process date.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp				processed;

	// Document state.
	@Column(nullable = false,name="document_state")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private EdiDocumentStatusEnum	documentStateEnum;

	public EdiDocumentLocator() {

	}

	public final String getDefaultDomainIdPrefix() {
		return "DOC";
	}

	public final Integer getIdDigits() {
		return 9;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<EdiDocumentLocator> getDao() {
		return DAO;
	}

	public Facility getFacility() {
		return getParent().getFacility();
	}

	public String getParentEdiServiceID() {
		return getParent().getDomainId();
	}

	public static void setDao(ITypedDao<EdiDocumentLocator> inEdiDocumentLocatorDao) {
		EdiDocumentLocator.DAO = inEdiDocumentLocatorDao; 
		
	}

}
