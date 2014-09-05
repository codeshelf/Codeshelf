/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectABC.java,v 1.34 2013/04/14 17:51:29 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.WordUtils;
import org.atteo.classindex.IndexSubclasses;
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

//@Entity
//@CacheStrategy(useBeanCache = true)
@MappedSuperclass
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@IndexSubclasses
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonPropertyOrder({ "domainId", "fullDomainId" })
@JsonIgnoreProperties({"className"})
@ToString(doNotUseGetters = true, of = { "domainId" })
public abstract class DomainObjectABC implements IDomainObject {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectABC.class);

	// This is the internal GUID for the object.
	@Id
	// @GeneratedValue(strategy = GenerationType.AUTO, generator = "com.gadgetworks.codeshelf.model.dao.UuidGenGw")
	@NonNull
	@Column(name = "persistentid", nullable = false)
	@Getter
	@Setter
	@JsonProperty
	// private UUID persistentId;
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
	@NonNull
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			version;

	public DomainObjectABC() {
		//		lastDefaultSequenceId = 0;
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
			if (this.getClass().equals(inObject.getClass())) {
				result = (persistentId.equals(((DomainObjectABC) inObject).persistentId));
			}
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
	private void addDeclaredAndInheritedFields(Class<?> inClass, Collection<Field> inFields) {
		inFields.addAll(Arrays.asList(inClass.getDeclaredFields()));
		Class<?> superClass = inClass.getSuperclass();
		if (superClass != null) {
			addDeclaredAndInheritedFields(superClass, inFields);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * This allows us to get a domain object field value from the DAO in a way that goes around Ebean getter/setter decoration.
	 * DO NOT CALL THIS METHOD OUTSIDE OF DAO STORE().
	 * @param inFieldName
	 * @return
	 */
	public final Object getFieldValueByName(final String inFieldName) {
		throw new NotImplementedException();
		/*
		Object result = null;

		boolean interceptingWas = ((EntityBean) this)._ebean_getIntercept().isIntercepting();
		((EntityBean) this)._ebean_getIntercept().setIntercepting(false);
		try {
			Method method = getClass().getMethod("get" + WordUtils.capitalize(inFieldName), (Class<?>[]) null);
			result = method.invoke(this, (Object[]) null);
		} catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | SecurityException e) {
			LOGGER.error("", e);
		}
		((EntityBean) this)._ebean_getIntercept().setIntercepting(interceptingWas);

		return result;
		*/
	}

	// --------------------------------------------------------------------------
	/**
	 * This allows us to set a domain object field value from the DAO in a way that goes around Ebean getter/setter decoration.
	 * DO NOT CALL THIS METHOD OUTSIDE OF DAO STORE().
	 * @param inFieldName
	 * @param inFieldValue
	 */
	public final void setFieldValueByName(final String inFieldName, final Object inFieldValue) {
		try {
			Method method = getClass().getDeclaredMethod("set" + WordUtils.capitalize(inFieldName), inFieldValue.getClass());
			method.invoke(this, inFieldValue);
		} catch (InvocationTargetException | NoSuchMethodException | SecurityException | IllegalArgumentException
				| IllegalAccessException e) {
			LOGGER.error("", e);
		}
	}
}
