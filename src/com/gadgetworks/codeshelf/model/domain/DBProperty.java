/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DBProperty.java,v 1.6 2012/09/23 03:05:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "DBPROPERTY")
@CacheStrategy
public class DBProperty extends DomainObjectABC {

	@Inject
	public static ITypedDao<DBProperty>	DAO;

	@Singleton
	public static class DBPropertyDao extends GenericDaoABC<DBProperty> implements ITypedDao<DBProperty> {
		public final Class<DBProperty> getDaoClass() {
			return DBProperty.class;
		}
	}

	public static final String	DB_SCHEMA_VERSION	= "SCHMAVER";

	@Getter
	@Setter
	@Column(nullable = false)
	private String				valueStr;

	public DBProperty() {
		super();
	}

	public final ITypedDao<DBProperty> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "DB";
	}

	// --------------------------------------------------------------------------
	/**
	 * DBProperties don't belong to anyone.
	 * @return
	 */
	public final IDomainObject getParent() {
		return null;
	}

	public final void setParent(IDomainObject inParent) {

	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
