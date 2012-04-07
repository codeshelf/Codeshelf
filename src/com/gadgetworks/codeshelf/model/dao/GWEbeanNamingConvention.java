/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: GWEbeanNamingConvention.java,v 1.3 2012/04/07 19:42:16 jeffw Exp $
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
