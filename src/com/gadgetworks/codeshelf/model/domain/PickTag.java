/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTag.java,v 1.5 2012/10/21 02:02:17 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
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
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class PickTag extends WirelessDevice {

	private static final Log	LOGGER	= LogFactory.getLog(PickTag.class);

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private short				serialBusPosition;

	public PickTag() {
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
