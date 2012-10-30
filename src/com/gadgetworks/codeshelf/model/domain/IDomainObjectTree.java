/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDomainObjectTree.java,v 1.1 2012/10/30 15:21:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

/**
 * @author jeffw
 *
 */
public interface IDomainObjectTree<P extends IDomainObject> extends IDomainObject {

	String	PARENT_ID_COLUMN_NAME	= "parent.persistentId";

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	P getParent();

	// --------------------------------------------------------------------------
	/**
	 * @param inParent
	 */
	void setParent(P inParent);

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
	 * Return the full domain ID (that includes the "dotted" domain ID of each parent up to the top of the hierarchy).
	 * @return
	 */
	String getFullDomainId();

}
