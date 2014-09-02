/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentLocator.java,v 1.22 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.EdiDocumentStatusEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.services.PersistencyService;
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
//@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class EdiDocumentLocator extends DomainObjectTreeABC<DropboxService> {

	@Inject
	public static ITypedDao<EdiDocumentLocator>	DAO;

	@Singleton
	public static class EdiDocumentLocatorDao extends GenericDaoABC<EdiDocumentLocator> implements ITypedDao<EdiDocumentLocator> {
		@Inject
		public EdiDocumentLocatorDao(PersistencyService persistencyService) {
			super(persistencyService);
		}
		
		public final Class<EdiDocumentLocator> getDaoClass() {
			return EdiDocumentLocator.class;
		}
	}

	private static final Logger		LOGGER	= LoggerFactory.getLogger(EdiDocumentLocator.class);

	// The owning EdiService.
	// We prefer to use the abstract class here, but it's not currently possible with Ebean.
	@ManyToOne(optional = false)
	private DropboxService			parent;

	// Document Path
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					documentPath;

	// Document Name
	@Column(nullable = true)
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
	@Column(nullable = false)
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

	public final ITypedDao<EdiDocumentLocator> getDao() {
		return DAO;
	}

	public final DropboxService getParent() {
		return parent;
	}

	public final void setParent(DropboxService inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return null; //getEdiTransactionDetails();
	}

	public final String getParentEdiServiceID() {
		return parent.getDomainId();
	}
}
