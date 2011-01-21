/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: GWEbeanNamingConvention.java,v 1.2 2011/01/21 01:12:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import com.avaje.ebean.config.AbstractNamingConvention;
import com.avaje.ebean.config.TableName;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class GWEbeanNamingConvention extends AbstractNamingConvention {

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getColumnFromProperty(java.lang.Class, java.lang.String)
	 */
	public String getColumnFromProperty(Class<?> beanClass, String inPropertyName) {
		return inPropertyName.toUpperCase();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getPropertyFromColumn(java.lang.Class, java.lang.String)
	 */
	public String getPropertyFromColumn(Class<?> beanClass, String inDbColumnName) {
		return inDbColumnName.toUpperCase();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.avaje.ebean.config.AbstractNamingConvention#getTableNameByConvention(java.lang.Class)
	 */
	@Override
	protected TableName getTableNameByConvention(Class<?> beanClass) {
		return new TableName(getCatalog(), getSchema(), beanClass.getSimpleName().toUpperCase());
	}

}
