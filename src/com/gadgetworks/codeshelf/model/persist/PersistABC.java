/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistABC.java,v 1.23 2012/06/27 05:07:51 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonProperty;

// --------------------------------------------------------------------------
/**
 *  This is the base persistent object.  It contains the persistence ID and provides
 *  operations around that ID, such as object equivalence (by ID).
 *  
 *  @author jeffw
 */

//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@MappedSuperclass
public abstract class PersistABC {

	// This is the internal GUID for the object.
	@Id
	@Column(nullable = false)
	@NonNull
	@Getter
	@Setter
	private Long		persistentId;
	// The domain ID
	@Column(nullable = false)
	@NonNull
	private String		domainId;
	// This is not an application-editable field.
	// It's for the private use of the ORM transaction system.
	@Version
	@Column(nullable = false)
	@Getter
	@Setter
	private Timestamp	version;

	public PersistABC() {
	}

	public abstract PersistABC getParent();

	public abstract void setParent(PersistABC inParent);

	// --------------------------------------------------------------------------
	/**
	 * @return	Return the name of the column used to store the domain key.  Used by EBean to find objects.
	 */
	public static String getIdColumnName() {
		return "domainId";
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
		PersistABC parentObject = getParent();
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

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public final boolean equals(Object inObject) {

		boolean result = false;

		if (inObject instanceof PersistABC) {
			if (this.getClass().equals(inObject.getClass())) {
				result = (persistentId.equals(((PersistABC) inObject).getPersistentId()));
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
}
