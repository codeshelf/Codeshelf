/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: GWEbeanNamingConvention.java,v 1.4 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import com.avaje.ebean.config.AbstractNamingConvention;
import com.avaje.ebean.config.TableName;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class GWEbeanNamingConvention extends AbstractNamingConvention {
	
	public GWEbeanNamingConvention() {
		super("{table}_SEQ");
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getColumnFromProperty(java.lang.Class, java.lang.String)
	 */
	public String getColumnFromProperty(Class<?> inBeanClass, String inPropertyName) {
		return inPropertyName.toUpperCase();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getPropertyFromColumn(java.lang.Class, java.lang.String)
	 */
	public String getPropertyFromColumn(Class<?> inBeanClass, String inDbColumnName) {
		return inDbColumnName.toUpperCase();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.avaje.ebean.config.AbstractNamingConvention#getTableNameByConvention(java.lang.Class)
	 */
	@Override
	protected TableName getTableNameByConvention(Class<?> inBeanClass) {
		return new TableName(getCatalog(), getSchema(), inBeanClass.getSimpleName().toUpperCase());
	}

}
