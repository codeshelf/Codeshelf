/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectABC.java,v 1.7 2012/07/30 17:44:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;

// --------------------------------------------------------------------------
/**
 *  This is the base persistent object.  It contains the persistence ID and provides
 *  operations around that ID, such as object equivalence (by ID).
 *  
 *  @author jeffw
 */

@MappedSuperclass
@ToString
public abstract class DomainObjectABC implements IDomainObject {

	private static final Log	LOGGER	= LogFactory.getLog(DomainObjectABC.class);

	// This is the internal GUID for the object.
	@Id
	@Column(nullable = false)
	@NonNull
	@Getter
	@Setter
	private Long				persistentId;
	// The domain ID
	@Column(nullable = false)
	@NonNull
	private String				domainId;
	// The last sequence used to generate a sequence ID.
	@Column(nullable = false)
	@NonNull
	@Getter
	@Setter
	private Integer				lastDefaultSequenceId;
	// This is not an application-editable field.
	// It's for the private use of the ORM transaction system.
	@Version
	@Column(nullable = false)
	@Getter
	@Setter
	private Timestamp			version;

	public DomainObjectABC() {
		lastDefaultSequenceId = 0;
	}

//	public String toString() {
//		String result = "";
//
//		result = "ID: " + getDomainId();
//
//		return result;
//	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getDefaultDomainId()
	 */
	@JsonIgnore
	public final String getDefaultDomainId() {

		String result = getDefaultDomainIdPrefix() + String.valueOf(System.currentTimeMillis());

		ITypedDao<IDomainObject> dao = getDao();

		IDomainObject parentObject = this.getParent();
		if ((parentObject != null) && (dao != null)) {

			String newID = null;

			boolean foundId = false;
			int maxTries = 100;

			do {
				Integer nextSeq = parentObject.getLastDefaultSequenceId() + 1;
				String testId = getDefaultDomainIdPrefix() + String.format("%02d", nextSeq);
				IDomainObject testIdObject = dao.findByDomainId(parentObject, testId);

				if (testIdObject == null) {
					foundId = true;
					result = testId;
					parentObject.setLastDefaultSequenceId(nextSeq);
					try {
						parentObject.getDao().store(parentObject);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}
			} while ((!foundId) && (maxTries > 0));
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	@JsonProperty
	public final String getClassName() {
		return this.getClass().getSimpleName();
	}

	// --------------------------------------------------------------------------
	/**
	 * To make sure the the domain object IDs are unique, we name them according to the parent object's get ID.
	 * This will result in a hierarchical ID naming scheme to guarantee that objects have a unique name.
	 * @param inParentObject
	 * @param inId
	 */
	public final void setDomainId(String inId) {
		IDomainObject parentObject = getParent();
		if (parentObject != null) {
			domainId = parentObject.getFullDomainId() + "." + inId;
		} else {
			domainId = inId;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the short domain ID for this object (that is unique among all of the objects under this parent).
	 * @return
	 */
	public final String getDomainId() {
		String result = "";

		int lastPeriodPos = domainId.lastIndexOf('.');
		if (lastPeriodPos == -1) {
			result = domainId;
		} else {
			result = domainId.substring(lastPeriodPos + 1);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the full domain ID (that includes the "dotted" domain ID of each parent up to the top of the hierarchy).
	 * @return
	 */
	public final String getFullDomainId() {
		return domainId;
	}
	
	public final Long getParentPersistentId() {
		Long result = null;
		IDomainObject domainObject = getParent();
		if (domainObject != null) {
			result = domainObject.getPersistentId();
		}
		return result;
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
