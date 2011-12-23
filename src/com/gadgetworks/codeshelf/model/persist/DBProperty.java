/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DBProperty.java,v 1.5 2011/12/23 23:21:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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

	@Getter
	@Setter
	@Column(nullable = false)
	private String								valueStr;

}
