/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiService.java,v 1.1 2012/09/06 06:43:38 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "EDISERVICE")
public class EdiService extends DomainObjectABC {

	@Inject
	public static ITypedDao<EdiService>	DAO;

	@Singleton
	public static class EdiServiceDao extends GenericDaoABC<EdiService> implements ITypedDao<EdiService> {
		public final Class<EdiService> getDaoClass() {
			return EdiService.class;
		}
	}

	private static final Log			LOGGER				= LogFactory.getLog(EdiService.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private Organization				parentOrganization;

	// The provider.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private EdiProviderEnum				providerEnum;

	// The provider.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private String						credentials;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentEdiService")
	@JsonIgnore
	@Getter
	private List<EdiDocumentLocator>	documentLocators	= new ArrayList<EdiDocumentLocator>();

	public EdiService() {

	}

	public final IDomainObject getParent() {
		return getParentOrganization();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Organization) {
			setParentOrganization((Organization) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return null; //getEdiDocuments();
	}

	public final void setParentOrganization(final Organization inParentOrganization) {
		parentOrganization = inParentOrganization;
	}

	@JsonIgnore
	public final ITypedDao<EdiService> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "EDI";
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addEdiDocumentLocator(EdiDocumentLocator inEdiDocumentLocator) {
		documentLocators.add(inEdiDocumentLocator);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeEdiDocumentLocator(EdiDocumentLocator inEdiDocumentLocator) {
		documentLocators.remove(inEdiDocumentLocator);
	}

}
