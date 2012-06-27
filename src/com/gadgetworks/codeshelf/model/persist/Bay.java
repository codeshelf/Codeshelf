/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.1 2012/06/27 05:07:51 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.experimental.Accessors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// --------------------------------------------------------------------------
/**
 * Bay
 * 
 * The object that models a storage bay in an aisle (pallet bay, etc.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "BAY")
@DiscriminatorValue("BAY")
public class Bay extends Location {

	private static final Log	LOGGER	= LogFactory.getLog(Bay.class);

	public Bay() {

	}

	public final PersistABC getParent() {
		return getParentLocation();
	}

	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Aisle) {
			setParentLocation((Aisle) inParent);
		}
	}

	public final Aisle getParentAisle() {
		return (Aisle) getParentLocation();
	}

	public final void setParentAisle(Aisle inParentAisle) {
		setParentLocation(inParentAisle);
	}
}
