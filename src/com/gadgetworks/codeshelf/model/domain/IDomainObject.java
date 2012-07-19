/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDomainObject.java,v 1.1 2012/07/19 06:11:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

/**
 * @author jeffw
 *
 */
public interface IDomainObject {

	String	ID_COLUMN_NAME	= "domainId";

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
	String getClassName();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	Long getPersistentId();

}
