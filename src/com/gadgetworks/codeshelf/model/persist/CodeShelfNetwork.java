/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetwork.java,v 1.4 2011/01/23 07:22:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.gadgetworks.codeshelf.application.Util;
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
@Table(name = "CODESHELFNETWORK")
public class CodeShelfNetwork extends PersistABC {

	//	private static final Log		LOGGER			= LogFactory.getLog(CodeShelfNetwork.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	// The network ID.
	@Column(nullable = false)
	private byte[]				mNetworkId;
	// The network description.
	@Column(nullable = false)
	private String				mDescription;
	// Active/Inactive network
	@Column(nullable = false)
	private boolean				mIsActive;
	// Network Id
	@Column(nullable = false)
	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "mParentCodeShelfNetwork")
	private List<ControlGroup>	mControlGroups		= new ArrayList<ControlGroup>();

	public CodeShelfNetwork() {
		mNetworkId = new byte[NetworkId.NETWORK_ID_BYTES];
		mDescription = "";
		mIsActive = true;
	}

	public final String toString() {
		return Arrays.toString(mNetworkId) + " " + mDescription;
	}

	public final NetworkId getNetworkId() {
		return new NetworkId(mNetworkId);
	}

	public final void setNetworkId(byte[] inNetworkId) {
		mNetworkId = inNetworkId;
	}

	public final String getDescription() {
		return mDescription;
	}

	public final void setDescription(String inDescription) {
		mDescription = inDescription;
	}

	public final boolean getIsActive() {
		return mIsActive;
	}

	public final void setIsActive(boolean inIsActive) {
		mIsActive = inIsActive;
	}

	// We always need to return the object cached in the DAO.
	public final List<ControlGroup> getControlGroups() {
		if (ISystemDAO.USE_CACHE) {
			List<ControlGroup> result = new ArrayList<ControlGroup>();
			if (!Util.getSystemDAO().isObjectPersisted(this)) {
				result = mControlGroups;
			} else {
				for (ControlGroup controlGroup : Util.getSystemDAO().getControlGroups()) {
					if (controlGroup.getParentCodeShelfNetwork().equals(this)) {
						result.add(controlGroup);
					}
				}
			}
			return result;
		} else {
			return mControlGroups;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(ControlGroup inControlGroup) {
		mControlGroups.add(inControlGroup);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(ControlGroup inControlGroup) {
		mControlGroups.remove(inControlGroup);
	}
}
