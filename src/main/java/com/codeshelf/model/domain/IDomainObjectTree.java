/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDomainObjectTree.java,v 1.3 2012/12/25 10:48:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

/**
 * @author jeffw
 *
 */
public interface IDomainObjectTree<P extends IDomainObject> extends IDomainObject {

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
	UUID getParentPersistentId();

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
