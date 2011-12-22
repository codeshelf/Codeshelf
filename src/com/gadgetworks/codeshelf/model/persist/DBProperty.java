/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DBProperty.java,v 1.4 2011/12/22 11:46:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.gadgetworks.codeshelf.model.dao.GenericDao;

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

	public static final String					DB_SCHEMA_VERSION	= "SCHMAVER";

	public static final GenericDao<DBProperty>	DAO					= new GenericDao<DBProperty>(DBProperty.class);

	@Column(name = "valueStr", nullable = false)
	private String								mValueStr;

	public DBProperty() {
		mValueStr = "";
	}

	public String toString() {
		return getId();
	}

	public String getValueStr() {
		return mValueStr;
	}

	public void setValueStr(String inValueStr) {
		mValueStr = inValueStr;
	}

}
