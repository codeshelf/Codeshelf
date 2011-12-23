/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroup.java,v 1.13 2011/12/23 23:21:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.NetGroup;
import com.gadgetworks.codeshelf.model.TagProtocolEnum;
import com.gadgetworks.codeshelf.model.dao.ISystemDAO;
import com.gadgetworks.codeshelf.server.tags.IControllerConnection;

// --------------------------------------------------------------------------
/**
 * This is the persistence object that contains information about the control group associated with a CodeShelf network.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "CONTROLGROUP")
public class ControlGroup extends PersistABC {

	private static final long		serialVersionUID	= -4923129546531851147L;

	// The owning CodeShelf network.
	@Getter
	@Setter
	@Column(nullable = false)
	@ManyToOne
	private CodeShelfNetwork		parentCodeShelfNetwork;
	// The control group ID
	@Column(nullable = false)
	private byte[]					controlGroupId;
	// The control group description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String					description;
	// Interface port number
	@Getter
	@Setter
	@Column(nullable = false)
	private short					interfacePortNum;
	// Active/Inactive rule
	@Getter
	@Setter
	@Column(nullable = false)
	private boolean					isActive;
	// Active/Inactive rule
	@Getter
	@Setter
	@Column(nullable = false)
	private TagProtocolEnum			tagProtocolEnum;
	// For a control group this is a list of all of the pick tags that belong in the set.
	@OneToMany(mappedBy = "parentControlGroup")
	private List<PickTag>			pickTags			= new ArrayList<PickTag>();

	@Transient()
	private IControllerConnection	controllerConnection;

	public ControlGroup() {
		parentCodeShelfNetwork = null;
		controlGroupId = new byte[NetGroup.NET_GROUP_BYTES];
		description = "";
		isActive = true;
		tagProtocolEnum = TagProtocolEnum.ATOP;
	}

	//	public String toString() {
	//		return mParentCodeShelfNetwork.toString() + "->" + getId().toString() + " " + mDescription;
	//	}
	//
	//	public CodeShelfNetwork getParentCodeShelfNetwork() {
	//		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
	//		if (mParentCodeShelfNetwork != null) {
	//			mParentCodeShelfNetwork = Util.getSystemDAO().loadCodeShelfNetwork(mParentCodeShelfNetwork.getPersistentId());
	//		}
	//		return mParentCodeShelfNetwork;
	//	}
	//
	//	public void setParentCodeShelfNetwork(CodeShelfNetwork inCodeShelfNetwork) {
	//		mParentCodeShelfNetwork = inCodeShelfNetwork;
	//	}

	public final NetGroup getControlGroupId() {
		return new NetGroup(controlGroupId);
	}

	public final void setControlGroupId(NetGroup inId) {
		controlGroupId = inId.getParamValueAsByteArray();
	}

	//	public final String getDescription() {
	//		return mDescription;
	//	}
	//
	//	public final void setDescription(String inDescription) {
	//		mDescription = inDescription;
	//	}
	//
	//	public final short getInterfacePortNum() {
	//		return mInterfacePortNum;
	//	}
	//
	//	public final void setInterfacePortNum(short inPortNumber) {
	//		mInterfacePortNum = inPortNumber;
	//	}
	//
	//	public final boolean getIsActive() {
	//		return mIsActive;
	//	}
	//
	//	public final void setIsActive(boolean inIsActive) {
	//		mIsActive = inIsActive;
	//	}

	public TagProtocolEnum getTagProtocol() {
		TagProtocolEnum result = tagProtocolEnum;
		if (result == null) {
			result = TagProtocolEnum.getTagProtocolEnum(0); //INVALID;
		}
		return result;
	}

	public void setTagProtocol(TagProtocolEnum inTagProtocolEnum) {
		tagProtocolEnum = inTagProtocolEnum;
	}

	// We always need to return the object cached in the DAO.
	public final List<PickTag> getPickTags() {
		if (ISystemDAO.USE_CACHE) {
			List<PickTag> result = new ArrayList<PickTag>();
			if (!Util.getSystemDAO().isObjectPersisted(this)) {
				result = pickTags;
			} else {
				for (PickTag pickTag : Util.getSystemDAO().getPickTags()) {
					if (pickTag.getParentControlGroup().equals(this)) {
						result.add(pickTag);
					}
				}
			}
			return result;
		} else {
			return pickTags;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addPickTag(PickTag inPickTag) {
		pickTags.add(inPickTag);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removePickTag(PickTag inPickTag) {
		pickTags.remove(inPickTag);
	}

	// --------------------------------------------------------------------------
	/**
	 * Atop tags do not have unique IDs.  Instead they are "numbered" on a serial bus from 1-to-200.
	 * The host s/w must remember the bus number for each device on the controller.
	 * We, on the other hand, have a MAC for each device.  We address commands to the MAC address
	 * of the device (anywhere on the network).
	 * @param inSerialBusNumber
	 * @return
	 */
	public PickTag getPickTagBySerialBusNumber(short inSerialBusNumber) {

		PickTag result = null;

		// To deal with the mismatch here, we maintain a mapping from the serial bus order to the MAC address.
		for (PickTag tag : getPickTags()) {
			if (tag.getSerialBusPosition() == inSerialBusNumber) {
				result = tag;
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public IControllerConnection getControllerConnection() {
		return controllerConnection;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inControllerConnection
	 */
	public void setControllerConnection(IControllerConnection inControllerConnection) {
		controllerConnection = inControllerConnection;
	}
}
