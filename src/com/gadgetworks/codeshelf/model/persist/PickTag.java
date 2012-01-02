/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTag.java,v 1.10 2012/01/02 11:43:18 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.CommandControlABC;

// --------------------------------------------------------------------------
/**
 * PickTag
 * 
 * A PickTag is a wireless device for industrial control.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "WIRELESSDEVICE")
@DiscriminatorValue("PICKTAG")
public class PickTag extends WirelessDevice {

	private static final long	serialVersionUID	= 501063502863299855L;

	private static final Log	LOGGER				= LogFactory.getLog(PickTag.class);

	@Getter
	@Setter
	@Column(nullable = false)
	private short				serialBusPosition;
	@ManyToOne
	@Column(nullable = false)
	private ControlGroup		parentControlGroup;

	public PickTag() {
		super();
	}

	public final ControlGroup getParentControlGroup() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (parentControlGroup != null) {
			parentControlGroup = ControlGroup.DAO.loadByPersistentId(parentControlGroup.getPersistentId());
		}
		return parentControlGroup;
	}

	public final void setParentControlGroup(ControlGroup inControlGroup) {
		parentControlGroup = inControlGroup;
	}

	/* --------------------------------------------------------------------------
	* (non-Javadoc)
	* @see com.gadgetworks.controller.IControllerListener#commandControlReceived(com.gadgetworks.command.CommandControlABC, com.gadgetworks.command.NetAddress)
	*/
	public final void controlCommandReceived(CommandControlABC inCommand) {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#buttonPressed(com.gadgetworks.actor.IActor, byte)
	 */
	public final void buttonCommandReceived(final byte inButtonNumberPressed, final byte inFunctionType) {
		LOGGER.debug(this.toString() + ": button " + inButtonNumberPressed);
	}

}
