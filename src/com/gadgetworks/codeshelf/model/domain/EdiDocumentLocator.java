/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentLocator.java,v 1.9 2012/10/02 03:17:58 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.EdiDocumentStatusEnum;
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
@CacheStrategy
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
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JsonIgnore
	private DropboxService			parent;

	// Document Path
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	@Setter
	private String					documentPath;

	// Document Name
	@Column(nullable = true)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	@Setter
	private String					documentName;

	// Received date.
	@Column(nullable = false)
	@Getter
	@Setter
	private Timestamp				received;

	// Process date.
	@Column(nullable = true)
	@Getter
	@Setter
	private Timestamp				processed;

	// Document state.
	@Getter
	@Setter
	@Column(nullable = false)
	private EdiDocumentStatusEnum	documentStateEnum;

	public EdiDocumentLocator() {

	}

	public final String getDefaultDomainIdPrefix() {
		return "DOC";
	}

	public final Integer getIdDigits() {
		return 9;
	}

	@JsonIgnore
	public final ITypedDao<EdiDocumentLocator> getDao() {
		return DAO;
	}

	public final EdiServiceABC getParentEdiService() {
		return parent;
	}

	public final void setParentEdiService(final DropboxService inEdiService) {
		parent = inEdiService;
	}

	public final IDomainObject getParent() {
		return getParentEdiService();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof DropboxService) {
			setParentEdiService((DropboxService) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return null; //getEdiTransactionDetails();
	}

	public final String getParentEdiServiceID() {
		return getParentEdiService().getDomainId();
	}
}
