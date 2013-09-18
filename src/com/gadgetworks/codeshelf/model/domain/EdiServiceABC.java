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
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;

// --------------------------------------------------------------------------
/**
 * EDI Service
 * 
 * An EDI service allows the system to import/export EDI documentLocators to/from other systems.
 * 
 * @author jeffw
 */

@Entity
@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "edi_service")
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ABC")
//@ToString(doNotUseGetters = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public abstract class EdiServiceABC extends DomainObjectTreeABC<Facility> implements IEdiService {

	private static final Logger			LOGGER				= LoggerFactory.getLogger(EdiServiceABC.class);

	// The owning Facility.
	@Column(nullable = false)
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

	//	public final Facility getParentFacility() {
	//		IDomainObject theParent = getParent();
	//		if (theParent instanceof Facility) {
	//			return (Facility) theParent;
	//		} else {
	//			return null;
	//		}
	//	}
	//
	//	public final void setParentFacility(final Facility inFacility) {
	//		setParent(inFacility);
	//	}

	public final List<? extends IDomainObject> getChildren() {
		return null; //getEdiDocuments();
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
