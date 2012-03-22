/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.10 2012/03/22 20:17:06 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

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
@Table(name = "AISLE")
public class Aisle extends PersistABC {

	private static final Log				LOGGER				= LogFactory.getLog(Aisle.class);

	// The owning facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private Facility						parentFacility;

	public Aisle() {
		parentFacility = null;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final PersistABC getParent() {
		return getParentFacility();
	}

//	public final Facility getParentFacility() {
//		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
//		if (parentFacility != null) {
//			FacilityDao dao = new FacilityDao();
//			parentFacility = dao.loadByPersistentId(parentFacility.getPersistentId());
//		}
//		return parentFacility;
//	}

//	public final void setParentFacility(Facility inParentFacility) {
//		parentFacility = inParentFacility;
//	}
}
