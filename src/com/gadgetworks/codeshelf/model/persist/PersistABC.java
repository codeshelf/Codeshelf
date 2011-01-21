/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistABC.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.sql.Timestamp;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

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
public abstract class PersistABC {

	@Id
	private Integer		mPersistentId;
	// This is not an application-editable field.
	// It's for the private use of the ORM transaction system.
	@Version
	private Timestamp	mVersion;

	public PersistABC() {
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final Integer getPersistentId() {
		return mPersistentId;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inPersistentID
	 */
	public final void setPersistentId(Integer inPersistentId) {
		mPersistentId = inPersistentId;
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final Timestamp getVersion() {
		return mVersion;
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  @param inTimestamp
	 */
	public final void setVersion(Timestamp inTimestamp) {
		mVersion = inTimestamp;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public final boolean equals(Object inObject) {
		if (inObject instanceof PersistABC) {
			return (mPersistentId == ((PersistABC) inObject).getPersistentId());
		} else {
			return super.equals(inObject);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public final int hashCode() {
		return mPersistentId.hashCode();
	}
}
