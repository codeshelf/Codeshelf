/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.1 2011/12/22 11:46:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;
import com.gadgetworks.codeshelf.model.dao.ISystemDAO;

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
@Table(name = "FACILITY")
public class Facility extends PersistABC {

	//	private static final Log		LOGGER			= LogFactory.getLog(CodeShelfNetwork.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	// The facility description.
	@Column(name="description", nullable = false)
	private String				mDescription;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "mParentFacility")
	private List<Aisle>	mAisles		= new ArrayList<Aisle>();

	public Facility() {
		mDescription = "";
	}

	public final String toString() {
		return getId() + " " + mDescription;
	}

	public final String getDescription() {
		return mDescription;
	}

	public final void setDescription(String inDescription) {
		mDescription = inDescription;
	}
	
	// We always need to return the object cached in the DAO.
	public final List<Aisle> getControlGroups() {
		if (ISystemDAO.USE_CACHE) {
			List<Aisle> result = new ArrayList<Aisle>();
			if (!Util.getSystemDAO().isObjectPersisted(this)) {
				result = mAisles;
			} else {
				for (Aisle aisle : Util.getSystemDAO().getAisles()) {
					if (aisle.getParentFacility().equals(this)) {
						result.add(aisle);
					}
				}
			}
			return result;
		} else {
			return mAisles;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(Aisle inAisle) {
		mAisles.add(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(Aisle inAisle) {
		mAisles.remove(inAisle);
	}
}
