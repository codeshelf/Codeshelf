/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistABC.java,v 1.16 2012/03/27 03:12:19 jeffw Exp $
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
//@Entity
//@Data
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
	@Getter
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
		return this.getClass().getName();
	}

	// --------------------------------------------------------------------------
	/**
	 * To make sure the the domain object IDs are unique, we name them according to the parent object's get ID.
	 * This will result in a hierarchical ID naming scheme to guarantee that objects have a unique name.
	 * @param inParentObject
	 * @param inId
	 */
	public final void setDomainId(final PersistABC inParentObject, String inId) {
		if (inParentObject != null) {
			domainId = inParentObject.getDomainId() + "." + inId;
		} else {
			domainId = inId;
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public final boolean equals(Object inObject) {

		boolean result = false;

		if (inObject instanceof PersistABC) {
			if (this.getClass().equals(inObject.getClass())) {
				result = (persistentId == ((PersistABC) inObject).getPersistentId());
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
