/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.1 2012/06/27 05:07:51 jeffw Exp $
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
 * Tier
 * 
 * The object that models a tier within a bay.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "LOCATION")
@DiscriminatorValue("TIER")
public class Tier extends Location {

	private static final Log	LOGGER	= LogFactory.getLog(Tier.class);

	public Tier() {

	}
	
	public final PersistABC getParent() {
		return getParentBay();
	}
	
	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Bay) {
			setParentLocation((Bay) inParent);
		}
	}
	
	public final Bay getParentBay() {
		return (Bay) getParentLocation();
	}
	
	public final void setParentBay(Bay inParentBay) {
		setParentLocation(inParentBay);
	}
}
