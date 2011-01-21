/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: SnapNetwork.java,v 1.3 2011/01/21 02:22:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
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
import com.gadgetworks.codeshelf.model.dao.ISystemDAO;

// --------------------------------------------------------------------------
/**
 * Ruleset
 * 
 * The Ruleset object holds information about rules to process against a remote source of events/notifications.
 *  * 
 * @author jeffw
 */

@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
//@MappedSuperclass
@Entity
@Table(name = "RULESET")
@DiscriminatorValue("REAL")
public class SnapNetwork extends PersistABC {

	//	private static final Log		LOGGER			= LogFactory.getLog(Ruleset.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	// The network ID.
	@Column(nullable = false)
	private String				mId;
	// The network description.
	@Column(nullable = false)
	private String				mDescription;
	// Active/Inactive network
	@Column(nullable = false)
	private boolean				mIsActive;
	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "mParentRuleset")
	private List<ControlGroup>	mControlGroups				= new ArrayList<ControlGroup>();

	// For a Ruleset this is a list of all of the rules that belong in the set.

	public SnapNetwork() {
		mId = "";
		mDescription = "";
		mIsActive = true;
	}

	public final String toString() {
		return mId + " " + mDescription;
	}

	public final String getId() {
		return mId;
	}

	public final void setId(String inId) {
		mId = inId;
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
					if (controlGroup.getParentSnapNetwork().equals(this)) {
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
