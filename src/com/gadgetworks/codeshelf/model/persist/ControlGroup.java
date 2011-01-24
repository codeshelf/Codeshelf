/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroup.java,v 1.7 2011/01/24 07:22:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.NetGroup;
import com.gadgetworks.codeshelf.model.dao.ISystemDAO;

// --------------------------------------------------------------------------
/**
 * This is the persistence object that contains information about the control group associated with a CodeShelf network.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "CONTROLGROUP")
public class ControlGroup extends PersistABC {

	private static final long	serialVersionUID	= -4923129546531851147L;

	// The owning CodeShelf network.
	@Column(nullable = false)
	@ManyToOne
	private CodeShelfNetwork	mParentCodeShelfNetwork;
	// The control group ID
	@Column(nullable = false)
	private byte[]				mId;
	// The control group description.
	@Column(nullable = false)
	private String				mDescription;
	// Active/Inactive rule
	@Column(nullable = false)
	private boolean				mIsActive;
	// For a control group this is a list of all of the pick tags that belong in the set.
	@OneToMany(mappedBy = "mParentControlGroup")
	private List<PickTag>		mPickTags			= new ArrayList<PickTag>();

	public ControlGroup() {
		mParentCodeShelfNetwork = null;
		mId = new byte[NetGroup.NET_GROUP_BYTES];
		mDescription = "";
		mIsActive = true;
	}

	public String toString() {
		return mParentCodeShelfNetwork.toString() + "->" + getId().toString() + " " + mDescription;
	}

	public CodeShelfNetwork getParentCodeShelfNetwork() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (mParentCodeShelfNetwork != null) {
			mParentCodeShelfNetwork = Util.getSystemDAO().loadCodeShelfNetwork(mParentCodeShelfNetwork.getPersistentId());
		}
		return mParentCodeShelfNetwork;
	}

	public void setParentCodeShelfNetwork(CodeShelfNetwork inCodeShelfNetwork) {
		mParentCodeShelfNetwork = inCodeShelfNetwork;
	}

	public NetGroup getId() {
		return new NetGroup(mId);
	}

	public void setId(NetGroup inId) {
		mId = inId.getParamValueAsByteArray();
	}

	public final String getDescription() {
		return mDescription;
	}

	public final void setDescription(String inDescription) {
		mDescription = inDescription;
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
