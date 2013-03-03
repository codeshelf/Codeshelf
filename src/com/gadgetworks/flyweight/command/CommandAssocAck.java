/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocAck.java,v 1.2 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;
import com.gadgetworks.flyweight.bitfields.NBitInteger;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *  The controller sends the associate ack command to any device whose GUID is under management by this controller.  
 *  If the controller is not now managing this device it will send a not-managed flag to indicate that the remote
 *  should attempt to (re)associate.  This would happen when the user shuts down the controller while the remote was
 *  associated.  (The remote never shutsdown, but it might crash/reboot.)
 *  
 *  There is a possibility that no controller will have the remote device's GUID under management.  It's also possible
 *  that the controller that *does* manage the remote device's GUID is not running.  In either case the remote will
 *  not receive any response.  The remote must periodically retry as necessary.
 */
public final class CommandAssocAck extends CommandAssocABC {

	public static final byte	IS_ASSOCIATED			= 0;
	public static final byte	IS_NOT_ASSOCIATED		= 1;

	public static final byte	ASSOCIATE_STATE_BITS	= 8;

	private static final Logger	LOGGER					= LoggerFactory.getLogger(CommandAssocAck.class);
	private NBitInteger			mAssociatedState		= new NBitInteger(ASSOCIATE_STATE_BITS, IS_NOT_ASSOCIATED);
	private int					mUnixTime;

	// --------------------------------------------------------------------------
	/**
	 *  @param inEndpoint	The endpoint to send the command.
	 *  @param inUniqueID	The GUID of the device. 
	 *  @param inAddressToAssign	The network address to assign to the device.
	 */
	public CommandAssocAck(final String inUniqueID, final NBitInteger inAssociatedState) {
		super(new NetCommandId(ASSOC_ACK_COMMAND), inUniqueID);

		GregorianCalendar cal = new GregorianCalendar();

		mAssociatedState = inAssociatedState;
		mUnixTime = (int) ((cal.getTime().getTime() + cal.getTimeZone().getOffset(cal.getTime().getTime())) / 1000);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public CommandAssocAck() {
		super(new NetCommandId(ASSOC_ACK_COMMAND));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return Integer.toHexString(ASSOC_RESP_COMMAND) + " ACK" + super.doToString() + " state=" + mAssociatedState + " unix time=" + new Date((long) mUnixTime * 1000);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		// First write the unique ID.
		try {
			inOutputStream.writeNBitInteger(mAssociatedState);
			inOutputStream.roundOutByte();
			inOutputStream.writeInt(mUnixTime);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			inInputStream.readNBitInteger(mAssociatedState);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + (ASSOCIATE_STATE_BITS / 8) + (Integer.SIZE / 8);
	}

}
