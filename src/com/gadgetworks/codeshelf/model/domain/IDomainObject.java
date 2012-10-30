/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDomainObject.java,v 1.14 2012/10/30 15:21:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;

/**
 * @author jeffw
 *
 */
public interface IDomainObject {

	String	ID_COLUMN_NAME			= "domainId";

	// --------------------------------------------------------------------------
	/**
	 * The prefix to use when creating a default domain ID.
	 * @return
	 */
	String getDefaultDomainIdPrefix();

	// --------------------------------------------------------------------------
	/**
	 * Return the short domain ID for this object (that is unique among all of the objects under this parent).
	 * @return
	 */
	String getDomainId();

	// --------------------------------------------------------------------------
	/**
	 * @param inId
	 */
	void setDomainId(String inId);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
//	List<? extends IDomainObject> getChildren();

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
	 * @param inPersistentId
	 */
	void setPersistentId(Long inPersistentId);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	<T extends IDomainObject> ITypedDao<T> getDao();

}
