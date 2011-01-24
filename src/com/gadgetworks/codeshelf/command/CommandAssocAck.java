/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocAck.java,v 1.3 2011/01/24 07:22:42 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetMacAddress;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *  The controller sends the associate ack command to any device whose MacAddr is under management by this controller.  
 *  If the controller is not now managing this device it will send a not-managed flag to indicate that the remote
 *  should attempt to (re)associate.  This would happen when the user shuts down the controller while the remote was
 *  associated.  (The remote never shutsdown, but it might crash/reboot.)
 *  
 *  There is a possibility that no controller will have the remote device's MacAddr under management.  It's also possible
 *  that the controller that *does* manage the remote device's MacAddr is not running.  In either case the remote will
 *  not receive any response.  The remote must periodically retry as necessary.
 */
public final class CommandAssocAck extends CommandAssocABC {

	public static final String	BEAN_ID				= "CommandAssocAck";

	public static final boolean	IS_ASSOCIATED		= true;
	public static final boolean	IS_NOT_ASSOCIATED	= false;

	private static final Log	LOGGER				= LogFactory.getLog(CommandAssocAck.class);

	private boolean				mAssociatedState	= IS_NOT_ASSOCIATED;
	private int					mUnixTime;

	// --------------------------------------------------------------------------
	/**
	 *  @param inEndpoint	The endpoint to send the command.
	 *  @param inMacAddress	The MacAddr of the device. 
	 */
	public CommandAssocAck(final NetMacAddress inMacAddress, final boolean inAssociatedState) {
		super(CommandIdEnum.ASSOC_ACK, inMacAddress);

		GregorianCalendar cal = new GregorianCalendar();

		mAssociatedState = inAssociatedState;
		mUnixTime = (int) ((cal.getTime().getTime() + cal.getTimeZone().getOffset(cal.getTime().getTime())) / 1000);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public CommandAssocAck() {
		super(CommandIdEnum.ASSOC_ACK);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return CommandIdEnum.ASSOC_ACK + super.doToString() + " state=" + mAssociatedState + " unix time="
				+ new Date((long) mUnixTime * 1000);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);

		// First write the unique ID.
		inTransport.setParam(new Boolean(mAssociatedState), 1);
		inTransport.setParam(new Long(mUnixTime), 2);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		mAssociatedState = ((Boolean) inTransport.getParam(1)).booleanValue();
		mUnixTime = ((Long) inTransport.getParam(2)).intValue();
	}
}
