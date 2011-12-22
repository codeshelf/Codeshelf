/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.1 2011/12/22 11:46:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.gadgetworks.codeshelf.application.Util;

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

	//	private static final Log		LOGGER			= LogFactory.getLog(CodeShelfNetwork.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	// The owning CodeShelf network.
	@Column(name = "parentFacility", nullable = false)
	@ManyToOne
	private Facility			mParentFacility;

	public Aisle() {
		mParentFacility = null;
	}

	public final String toString() {
		return getId();
	}

	public final Facility getParentFacility() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (mParentFacility != null) {
			mParentFacility = Util.getSystemDAO().loadFacility(mParentFacility.getPersistentId());
		}
		return mParentFacility;
	}

	public final void setParentFacility(Facility inParentFacility) {
		mParentFacility = inParentFacility;
	}
}
