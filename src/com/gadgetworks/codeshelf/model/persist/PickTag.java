/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTag.java,v 1.5 2011/01/24 07:22:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.command.CommandControlABC;

// --------------------------------------------------------------------------
/**
 * PickTag
 * 
 * A PickTag is a wireless device for industrial control.
 * 
 * @author jeffw
 */

/**
 * @author jeffw
 *
 */
@Entity
@Table(name = "WIRELESSDEVICE")
@DiscriminatorValue("PICKTAG")
public class PickTag extends WirelessDevice {

	private static final long	serialVersionUID	= 501063502863299855L;

	private static final Log	LOGGER				= LogFactory.getLog(PickTag.class);

	@Column(nullable = false)
	@ManyToOne
	private ControlGroup		mParentControlGroup;

	public PickTag() {
		super();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "PickTag: " + getMacAddress() + " " + getDescription();
	}

	public ControlGroup getParentControlGroup() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (mParentControlGroup != null) {
			mParentControlGroup = Util.getSystemDAO().loadControlGroup(mParentControlGroup.getPersistentId());
		}
		return mParentControlGroup;
	}

	public void setParentControlGroup(ControlGroup inControlGroup) {
		mParentControlGroup = inControlGroup;
	}

	/* --------------------------------------------------------------------------
	* (non-Javadoc)
	* @see com.gadgetworks.controller.IControllerListener#commandControlReceived(com.gadgetworks.command.CommandControlABC, com.gadgetworks.command.NetAddress)
	*/
	public void controlCommandReceived(CommandControlABC inCommand) {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#buttonPressed(com.gadgetworks.actor.IActor, byte)
	 */
	public final void buttonCommandReceived(final byte inButtonNumberPressed, final byte inFunctionType) {

		LOGGER.debug(this.toString() + ": button " + inButtonNumberPressed);

	}

}
