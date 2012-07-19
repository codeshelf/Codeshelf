/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ObjectIDComparer.java,v 1.2 2012/07/19 06:11:34 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.model;

import org.eclipse.jface.viewers.IElementComparer;

import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class ObjectIDComparer implements IElementComparer {

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IElementComparer#equals(java.lang.Object, java.lang.Object)
	 */
	public final boolean equals(Object inObjectA, Object inObjectB) {
		if ((inObjectA instanceof PersistABC) && (inObjectB instanceof PersistABC)) {
			return inObjectA.equals(inObjectB);
		} else {
			return inObjectA.equals(inObjectB);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IElementComparer#hashCode(java.lang.Object)
	 */
	public final int hashCode(Object inElement) {
		return inElement.hashCode();
	}
}
