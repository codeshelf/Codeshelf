/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocReq.java,v 1.2 2013/03/03 23:27:20 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  A command associate request is broadcast by a remote to all controllers on a channel.
 *  Only the controller that manages that remote device (known by its GUID) will send an associate response.
 *  
 *  @author jeffw
 */
public final class CommandAssocReq extends CommandAssocABC {

	public static final int		ASSOC_REQ_BYTES	= 2;

	public static final int		ASSOC_REQ_HW_VER_LEN = 4;	// 4 bytes
	public static final int		ASSOC_REQ_FW_VER_LEN = 4;	// 4 bytes
	
	private static final Logger	LOGGER			= LoggerFactory.getLogger(CommandAssocReq.class);

	//Note these need to be treated as unsigned
	private byte[]				hardwareVersion;
	private byte[]				firmwareVersion;
	private byte				radioProtocolVersion;
	private byte				systemStatus;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocReq(byte[] hardwareVersion,
		byte[] firmwareVersion,
		byte radioProtocolVersion,
		byte systemStatus,
		final String inRemoteGUID) {
		super(new NetCommandId(ASSOC_REQ_COMMAND), inRemoteGUID);

		this.hardwareVersion = hardwareVersion;
		this.firmwareVersion = firmwareVersion;
		this.radioProtocolVersion = radioProtocolVersion;
		this.systemStatus = systemStatus;

	}
	
	public CommandAssocReq(byte hardwareVersion,
		byte firmwareVersion,
		byte radioProtocolVersion,
		byte systemStatus,
		final String inRemoteGUID) {
		super(new NetCommandId(ASSOC_REQ_COMMAND), inRemoteGUID);

		this.hardwareVersion = new byte[] {hardwareVersion};
		this.firmwareVersion = new byte[] {firmwareVersion};
		this.radioProtocolVersion = radioProtocolVersion;
		this.systemStatus = systemStatus;

	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor useed to create an associate request command received from the network.
	 */
	public CommandAssocReq() {
		super(new NetCommandId(ASSOC_REQ_COMMAND));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	@Override
	public String doToString() {
		return Integer.toHexString(ASSOC_REQ_COMMAND) + " REQ" + super.doToString() + " hw=" + hardwareVersion.toString() + " fw="
				+ firmwareVersion.toString() + " rp=" + (radioProtocolVersion & 0xff) + " sys stat=0x"
				+ Integer.toHexString(systemStatus & 0xff);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	@Override
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			// Write the device version.
			inOutputStream.writeBytes(hardwareVersion);
			inOutputStream.writeBytes(firmwareVersion);
			inOutputStream.writeByte(radioProtocolVersion);
			inOutputStream.writeByte(systemStatus);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	@Override
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			// Read the hw,fw,rp and system status register
			if (inInputStream.readableBytes() >= 4) {
				hardwareVersion = new byte[ASSOC_REQ_HW_VER_LEN];
				inInputStream.readBytes(hardwareVersion, ASSOC_REQ_HW_VER_LEN);
			}
			if (inInputStream.readableBytes() >= 4) {
				firmwareVersion = new byte[ASSOC_REQ_FW_VER_LEN];
				inInputStream.readBytes(firmwareVersion, ASSOC_REQ_FW_VER_LEN);
			}
			if (inInputStream.readableBytes() >= 1) {
				radioProtocolVersion = inInputStream.readByte();
			}
			if (inInputStream.readableBytes() >= 1) {
				systemStatus = inInputStream.readByte();
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	@Override
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + ASSOC_REQ_BYTES;
	}

	public byte[] getHardwareVersion() {
		return hardwareVersion;
	}

	public byte[] getFirmwareVersion() {
		return firmwareVersion;
	}

	public byte getRadioProtocolVersion() {
		return radioProtocolVersion;
	}

	public byte getSystemStatus() {
		return systemStatus;
	}

}
