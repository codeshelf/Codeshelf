/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.13 2012/04/10 08:01:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// --------------------------------------------------------------------------
/**
 * CodeShelfNetwork
 * 
 * The CodeShelfNetwork object holds information about how to create a standalone CodeShelf network.
 * (There may be more than one running at a facility.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "LOCATION")
@DiscriminatorValue("AISLE")
public class Aisle extends Location {

	private static final Log	LOGGER	= LogFactory.getLog(Aisle.class);

	public Aisle() {

	}
	
	public final PersistABC getParent() {
		return getParentLocation();
	}
	
	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Facility) {
			setParentLocation((Facility) inParent);
		}
	}
	
	public final Facility getParentFacility() {
		return (Facility) getParentLocation();
	}
	
	public final void setParentFacility(Facility inParentFacility) {
		setParentLocation(inParentFacility);
	}
}
