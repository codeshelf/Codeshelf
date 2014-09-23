/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceABC.java,v 1.21 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistencyService;
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
//@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "edi_service")
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ABC")
//@ToString(doNotUseGetters = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class EdiServiceABC extends DomainObjectTreeABC<Facility> implements IEdiService {

	@SuppressWarnings("unused")
	private static final Logger			LOGGER				= LoggerFactory.getLogger(EdiServiceABC.class);

	@Inject
	public static ITypedDao<EdiServiceABC>	DAO;

	@Singleton
	public static class EdiServiceABCDao extends GenericDaoABC<EdiServiceABC> implements ITypedDao<EdiServiceABC> {
		@Inject
		public EdiServiceABCDao(PersistencyService persistencyService) {
			super(persistencyService);
		}

		public final Class<EdiServiceABC> getDaoClass() {
			return EdiServiceABC.class;
		}
	}

	
	// The owning Facility.
	@ManyToOne(optional = false)
	private Facility					parent;

	// The provider.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private EdiProviderEnum				providerEnum;

	// Service state.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private EdiServiceStateEnum			serviceStateEnum;

	// The credentials (encoded toekns or obfuscated keys only).
	@Column(nullable = true)
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

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;

	}

	public final List<? extends IDomainObject> getChildren() {
		return null; //getEdiDocuments();
	}

	public final String getDefaultDomainIdPrefix() {
		return "EDI";
	}

	@JsonProperty
	public abstract boolean getHasCredentials();
	
	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addEdiDocumentLocator(EdiDocumentLocator inEdiDocumentLocator) {
		documentLocators.add(inEdiDocumentLocator);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeEdiDocumentLocator(EdiDocumentLocator inEdiDocumentLocator) {
		documentLocators.remove(inEdiDocumentLocator);
	}
}
