/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DBProperty.java,v 1.3 2011/01/22 07:58:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Entity;

// --------------------------------------------------------------------------
/**
 * DBProperty
 * 
 * The DBProperty object holds version/process information regarding the underlying databse structures, versions, etc.
 * 
 * @author jeffw
 */

@Entity
public class DBProperty extends PersistABC {

	public static final String	DB_SCHEMA_VERSION	= "SCHMAVER";

	private String				mPropertyId;
	private String				mCurrentValueStr;

	public DBProperty() {
		mPropertyId = "";
		mCurrentValueStr = "";
	}

	public String toString() {
		return mPropertyId;
	}

	public String getPropertyId() {
		return mPropertyId;
	}

	public void setPropertyId(String inPropertyId) {
		mPropertyId = inPropertyId;
	}

	public String getValueStr() {
		return mCurrentValueStr;
	}

	public void setValueStr(String inValueStr) {
		mCurrentValueStr = inValueStr;
	}

}
