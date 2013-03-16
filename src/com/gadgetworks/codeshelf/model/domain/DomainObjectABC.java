/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectABC.java,v 1.31 2013/03/16 08:03:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.lang.WordUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.bean.EntityBean;

// --------------------------------------------------------------------------
/**
 *  This is the base persistent object.  It contains the persistence ID and provides
 *  operations around that ID, such as object equivalence (by ID).
 *  
 *  @author jeffw
 */

@Entity
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@JsonPropertyOrder({ "domainId", "fullDomainId" })
//@ToString(doNotUseGetters = true)
public abstract class DomainObjectABC implements IDomainObject {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectABC.class);

	// This is the internal GUID for the object.
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "com.gadgetworks.codeshelf.model.dao.UuidGenGw")
	@NonNull
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private UUID				persistentId;

	// The domain ID
	@NonNull
	@Column(nullable = false)
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
				result = (persistentId.equals(((DomainObjectABC) inObject).getPersistentId()));
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
	 * @param inPersistentId
	 * @param inClass
	 * @return
	 */
	public static <T extends DomainObjectABC> T findByPersistentId(Long inPersistentId, Class<T> inClass) {
		T result = null;
		try {
			result = Ebean.find(inClass, inPersistentId);
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public static <T extends DomainObjectABC> List<T> getAll(Class<T> inClass) {
		Query<T> query = Ebean.createQuery(inClass);
		//query = query.setUseCache(true);
		return query.findList();
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

		//		try {
		//			Collection<Field> fields = new ArrayList<Field>();
		//			addDeclaredAndInheritedFields(getClass(), fields);
		//			for (Field field : fields) {
		//				if (field.getName().equals(inFieldName)) {
		//					result = field.get(this);
		//				}	
		//			}
		//		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
		//			LOGGER.error("", e);
		//		}

		return result;
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
		} catch (InvocationTargetException | NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			LOGGER.error("", e);
		}
	}
}
