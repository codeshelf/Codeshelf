/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DBProperty.java,v 1.6 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.google.inject.Inject;

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

	public interface IDBPropertyDao extends IGenericDao<DBProperty> {		
	}
	
	public static class DBPropertyDao extends GenericDao<DBProperty> implements IDBPropertyDao {
		@Inject
		public DBPropertyDao(final IDaoRegistry inDaoRegistry) {
			super(DBProperty.class, inDaoRegistry);
		}
	}

	@Getter
	@Setter
	@Column(nullable = false)
	private String								valueStr;

}
