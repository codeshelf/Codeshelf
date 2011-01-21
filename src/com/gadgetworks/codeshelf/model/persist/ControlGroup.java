/*******************************************************************************
 *  HoobeeNet
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroup.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.dao.ISystemDAO;

// --------------------------------------------------------------------------
/**
 * This is the persistence object that contains information about the control group associated with a SNAP network.
 * 
 * @author jeffw
 */

@Entity
public final class ControlGroup extends PersistABC {

	private static final long	serialVersionUID	= -4923129546531851147L;

	// The owning Snap network.
	@Column(nullable = false)
	@ManyToOne
	private SnapNetwork			mParentSnapNetwork;
	// The control group ID
	@Column(nullable = false)
	private String				mId;
	// Active/Inactive rule
	@Column(nullable = false)
	private boolean				mIsActive;
	// For a control group this is a list of all of the pick tags that belong in the set.
	@OneToMany(mappedBy = "mParentControlGroup")
	private List<PickTag>		mPickTags			= new ArrayList<PickTag>();

	public ControlGroup() {
		mParentSnapNetwork = null;
		mId = "";
		mIsActive = true;
	}

	public String toString() {
		return mParentSnapNetwork.toString() + "->" + mId;
	}

	public SnapNetwork getParentSnapNetwork() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (mParentSnapNetwork != null) {
			mParentSnapNetwork = Util.getSystemDAO().loadSnapNetwork(mParentSnapNetwork.getPersistentId());
		}
		return mParentSnapNetwork;
	}

	public void setParentSnapNetwork(SnapNetwork inSnapNetwork) {
		mParentSnapNetwork = inSnapNetwork;
	}

	public String getId() {
		return mId;
	}

	public void setId(String inId) {
		mId = inId;
	}

	public boolean getIsActive() {
		return mIsActive;
	}

	public void setIsActive(boolean inIsActive) {
		mIsActive = inIsActive;
	}
	
	// We always need to return the object cached in the DAO.
	public final List<PickTag> getPickTags() {
		if (ISystemDAO.USE_CACHE) {
			List<PickTag> result = new ArrayList<PickTag>();
			if (!Util.getSystemDAO().isObjectPersisted(this)) {
				result = mPickTags;
			} else {
				for (PickTag pickTag : Util.getSystemDAO().getPickTags()) {
					if (pickTag.getParentControlGroup().equals(this)) {
						result.add(pickTag);
					}
				}
			}
			return result;
		} else {
			return mPickTags;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addPickTag(PickTag inPickTag) {
		mPickTags.add(inPickTag);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removePickTag(PickTag inPickTag) {
		mPickTags.remove(inPickTag);
	}

}
