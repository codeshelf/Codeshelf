/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceABC.java,v 1.7 2012/09/23 03:05:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.IEdiService;

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
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Table(name = "EDISERVICE")
@ToString
public abstract class EdiServiceABC extends DomainObjectABC implements IEdiService {

//	@Inject
//	public static EdiServiceDao	DAO;
//
//	@Singleton
//	public static class EdiServiceDao extends GenericDaoABC<EdiServiceABC> implements ITypedDao<EdiServiceABC> {
//		public final Class<EdiServiceABC> getDaoClass() {
//			return EdiServiceABC.class;
//		}
//	}

	private static final Log			LOGGER				= LogFactory.getLog(EdiServiceABC.class);

	// The owning Facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Facility					parent;

	// The provider.
	@Column(nullable = false)
	@Getter
	@Setter
	private EdiProviderEnum				providerEnum;

	// Service state.
	@Column(nullable = false)
	@Getter
	@Setter
	private EdiServiceStateEnum			serviceStateEnum;

	// The credentials (encoded toekns or obfuscated keys only).
	@Column(nullable = true)
	@Getter
	@Setter
	private String						providerCredentials;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent")
	@JsonIgnore
	@Getter
	private List<EdiDocumentLocator>	documentLocators	= new ArrayList<EdiDocumentLocator>();

	public EdiServiceABC() {

	}

	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	public final IDomainObject getParent() {
		return getParentFacility();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return null; //getEdiDocuments();
	}

	public final String getDefaultDomainIdPrefix() {
		return "EDISERVICE";
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
