/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentLocator.java,v 1.2 2012/09/08 03:03:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.EdiDocumentStateEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "EDIDOCUMENTLOCATOR")
public class EdiDocumentLocator extends DomainObjectABC {

	@Inject
	public static ITypedDao<EdiDocumentLocator>	DAO;

	@Singleton
	public static class EdiDocumentLocatorDao extends GenericDaoABC<EdiDocumentLocator> implements ITypedDao<EdiDocumentLocator> {
		public final Class<EdiDocumentLocator> getDaoClass() {
			return EdiDocumentLocator.class;
		}
	}

	private static final Log		LOGGER	= LogFactory.getLog(EdiDocumentLocator.class);

	// The owning EdiService.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private EdiServiceABC			parentEdiService;

	// Document ID
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private String					documentId;

	// Document Name
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private String					documentName;

	// Received date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp				received;

	// Process date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp				processed;

	// Document state.
	@Getter
	@Setter
	@Column(nullable = false)
	private EdiDocumentStateEnum	documentState;

	public EdiDocumentLocator() {

	}

	public final String getDefaultDomainIdPrefix() {
		return "EDI";
	}

	@JsonIgnore
	public final ITypedDao<EdiDocumentLocator> getDao() {
		return DAO;
	}

	public final IDomainObject getParent() {
		return getParentEdiService();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof EdiServiceABC) {
			setParentEdiService((EdiServiceABC) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return null; //getEdiTransactionDetails();
	}

	public final void setParentEdiService(final EdiServiceABC inEdiService) {
		parentEdiService = inEdiService;
	}

	public final String getParentEdiServiceID() {
		return getParentEdiService().getDomainId();
	}
}
