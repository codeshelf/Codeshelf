/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectABC.java,v 1.26 2012/12/15 02:25:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.annotation.CacheStrategy;

// --------------------------------------------------------------------------
/**
 *  This is the base persistent object.  It contains the persistence ID and provides
 *  operations around that ID, such as object equivalence (by ID).
 *  
 *  @author jeffw
 */

@Entity
@CacheStrategy
@ToString
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@JsonPropertyOrder({ "domainId", "fullDomainId" })
public abstract class DomainObjectABC implements IDomainObject {

	private static final Log	LOGGER	= LogFactory.getLog(DomainObjectABC.class);

	// This is the internal GUID for the object.
	@Id
	@NonNull
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Long				persistentId;

	// The domain ID
	@NonNull
	@Column(nullable = false)
	@JsonProperty
	@Getter
	@Setter
	private String				domainId;

	// This is not an application-editable field.
	// It's for the private use of the ORM transaction system.
	@Version
	@NonNull
	@Column(nullable = false)
	@Getter
	@Setter
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
	}
