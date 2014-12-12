/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectABC.java,v 1.34 2013/04/14 17:51:29 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.atteo.classindex.IndexSubclasses;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// --------------------------------------------------------------------------
/**
 *  This is the base persistent object.  It contains the persistence ID and provides
 *  operations around that ID, such as object equivalence (by ID).
 *  
 *  @author jeffw
 */

@MappedSuperclass
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@IndexSubclasses
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonPropertyOrder({ "domainId", "fullDomainId" })
@JsonIgnoreProperties({"className"})
@ToString(doNotUseGetters = true, of = { "domainId" })
public abstract class DomainObjectABC implements IDomainObject {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectABC.class);

	// This is the internal GUID
	@Id
	@NonNull
	@Column(name = "persistentid", nullable = false)
	@Getter
	@Setter
	@JsonProperty
	@Type(type="com.gadgetworks.codeshelf.platform.persistence.DialectUUIDType")
	private UUID persistentId = UUID.randomUUID();

	// The domain ID
	@NonNull
	@Column(name = "domainid", nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				domainId;

	// This is not an application-editable field.
	// It's for the private use of the ORM transaction system.
	@Version
	@Column(nullable = false)
	@Getter
	@Setter
	// @JsonProperty do we need to serialize this?
	private long version;

	public DomainObjectABC() {
		// lastDefaultSequenceId = 0;
	}

	public DomainObjectABC(String inDomainId) {
		this.domainId = inDomainId;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected Integer getIdDigits() {
		return 2;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	@JsonProperty
	public final String getClassName() {
		return this.getClass().getSimpleName();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public final boolean equals(Object inObject) {
		boolean result = false;

		if (inObject instanceof DomainObjectABC) {
			return (Objects.equals(this.getClass(), inObject.getClass())
					&& Objects.equals(this.persistentId, ((DomainObjectABC) inObject).persistentId));
		} else {
			result = super.equals(inObject);
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public final int hashCode() {
		if (persistentId != null) {
			return persistentId.hashCode();
		} else {
			return 0;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Get all of the fields up the tree.
	 * @param inClass
	 * @param inFields
	 */
	@SuppressWarnings("unused")
	private void addDeclaredAndInheritedFields(Class<?> inClass, Collection<Field> inFields) {
		inFields.addAll(Arrays.asList(inClass.getDeclaredFields()));
		Class<?> superClass = inClass.getSuperclass();
		if (superClass != null) {
			addDeclaredAndInheritedFields(superClass, inFields);
		}
	}

	public static IDomainObject deproxify(IDomainObject domainObject) {
		if (domainObject==null) {
			return null;
		}
	    if (domainObject instanceof HibernateProxy) {
	        Hibernate.initialize(domainObject);
	        DomainObjectABC realDomainObject = (DomainObjectABC) ((HibernateProxy) domainObject)
	                  .getHibernateLazyInitializer()
	                  .getImplementation();
	        return realDomainObject;
	    }
		return domainObject;
	}

	/*
	public static <T extends IDomainObject> T as(T domainObject) {
		if (domainObject==null) {
			return null;
		}
		domainObject = (T) deproxify(domainObject);
	    return domainObject;
	}
	*/		

}
