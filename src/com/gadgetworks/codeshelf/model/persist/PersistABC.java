/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistABC.java,v 1.6 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.sql.Timestamp;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import lombok.Data;
import lombok.NonNull;

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
@Data
public abstract class PersistABC {

	// This is the internal GUID for the object.
	@Id
	@Column(nullable = false)
	@NonNull
	private Long		persistentId;
	// The domain ID
	@Column(nullable = false)
	private String		id;
	// This is not an application-editable field.
	// It's for the private use of the ORM transaction system.
	@Version
	@Column(nullable = false)
	private Timestamp	version;

	public PersistABC() {
	}

	// --------------------------------------------------------------------------
	/**
	 * @return	Return the name of the column used to store the domain key.  Used by EBean to find objects.
	 */
	public static String getIdColumnName() {
		return "mId";
	}

	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  @return
	//	 */
	//	public  Integer getPersistentId() {
	//		return mPersistentId;
	//	}
	//
	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  @param inPersistentID
	//	 */
	//	public  void setPersistentId(Integer inPersistentId) {
	//		mPersistentId = inPersistentId;
	//	}
	//
	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  @return
	//	 */
	//	public  String getId() {
	//		return mId;
	//	}
	//
	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  @param inPersistentID
	//	 */
	//	public  void setId(String inId) {
	//		mId = inId;
	//	}
	//
	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  @return
	//	 */
	//	public  Timestamp getVersion() {
	//		return mVersion;
	//	}
	//
	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  @param inTimestamp
	//	 */
	//	public  void setVersion(Timestamp inTimestamp) {
	//		mVersion = inTimestamp;
	//	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object inObject) {

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
	public int hashCode() {
		if (persistentId != null) {
			return persistentId.hashCode();
		} else {
			return 0;
		}
	}
}
