/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DBProperty.java,v 1.13 2012/07/12 08:18:06 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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

	@Singleton
	public static class DBPropertyDao extends GenericDao<DBProperty> implements ITypedDao<DBProperty> {
		public DBPropertyDao() {
			super(DBProperty.class);
		}
	}

	@Inject
	public static ITypedDao<DBProperty> DAO;
//	public static GenericDao<DBProperty> DAO = new GenericDao<DBProperty>(DBProperty.class);

	public static final String					DB_SCHEMA_VERSION	= "SCHMAVER";

	@Getter
	@Setter
	@Column(nullable = false)
	private String								valueStr;
	
	// --------------------------------------------------------------------------
	/**
	 * DBProperties don't belong to anyone.
	 * @return
	 */
	public final PersistABC getParent() {
		return null;
	}
	
	public final void setParent(PersistABC inParent) {

	}
	

}
