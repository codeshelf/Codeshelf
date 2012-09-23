/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDomainObject.java,v 1.9 2012/09/23 03:05:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import com.avaje.ebean.bean.EntityBean;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;

/**
 * @author jeffw
 *
 */
public interface IDomainObject {

	String	ID_COLUMN_NAME	= "domainId";

	// --------------------------------------------------------------------------
	/**
	 * For constructors that don't pass a domain ID, create one.
	 * @return
	 */
	String computeDefaultDomainId();

	// --------------------------------------------------------------------------
	/**
	 * The prefix to use when creating a default domain ID.
	 * @return
	 */
	String getDefaultDomainIdPrefix();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	Integer getLastDefaultSequenceId();

	// --------------------------------------------------------------------------
	/**
	 * @param inLastDefaultSewquenceId
	 */
	void setLastDefaultSequenceId(Integer inLastDefaultSewquenceId);

	// --------------------------------------------------------------------------
	/**
	 * @param inId
	 */
	void setDomainId(String inId);

	// --------------------------------------------------------------------------
	/**
	 * Return the short domain ID for this object (that is unique among all of the objects under this parent).
	 * @return
	 */
	String getDomainId();

	// --------------------------------------------------------------------------
	/**
	 * Return the full domain ID (that includes the "dotted" domain ID of each parent up to the top of the hierarchy).
	 * @return
	 */
	String getFullDomainId();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	IDomainObject getParent();

	// --------------------------------------------------------------------------
	/**
	 * @param inParent
	 */
	void setParent(IDomainObject inParent);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	List<? extends IDomainObject> getChildren();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	Long getParentPersistentId();

	// --------------------------------------------------------------------------
	/**
	 * Return the parent object's full domain ID (that includes the "dotted" domain ID of each parent up to the top of the hieararchy).
	 * @return
	 */
	String getParentFullDomainId();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	String getClassName();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	Long getPersistentId();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	<T extends IDomainObject> ITypedDao<T> getDao();

}
