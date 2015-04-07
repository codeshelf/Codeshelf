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
import com.google.common.base.Charsets;

// --------------------------------------------------------------------------
/**
 *  A command associate request is broadcast by a remote to all controllers on a channel.
 *  Only the controller that manages that remote device (known by its GUID) will send an associate response.
 *  
 *  @author jeffw
 */
public final class CommandAssocReq extends CommandAssocABC {

	public static final int		ASSOC_REQ_BYTES	= 2;

	private static final Logger	LOGGER			= LoggerFactory.getLogger(CommandAssocReq.class);

	//Note these need to be treated as unsigned
	private String				hardwareVersion;
	private String				firmwareVersion;
	private byte				radioProtocolVersion;
	private byte				systemStatus;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocReq(String hardwareVersion,
		String firmwareVersion,
		byte radioProtocolVersion,
		byte systemStatus,
		final String inRemoteGUID) {
		super(new NetCommandId(ASSOC_REQ_COMMAND), inRemoteGUID);

		this.hardwareVersion = hardwareVersion;
		this.firmwareVersion = firmwareVersion;
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
		return Integer.toHexString(ASSOC_REQ_COMMAND) + " REQ" + super.doToString() + " hw=" + (hardwareVersion) + " fw="
				+ (firmwareVersion) + " rp=" + (radioProtocolVersion & 0xff) + " sys stat=0x"
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

			inOutputStream.writeBytes(hardwareVersion.getBytes(Charsets.US_ASCII), 4);
			inOutputStream.writeBytes(firmwareVersion.getBytes(Charsets.US_ASCII), 4);
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
			byte[] data = new byte[4];

			// Read the hw,fw,rp and system status register
			if (inInputStream.readableBytes() >= 4) {
				inInputStream.readBytes(data, 4);
				hardwareVersion = new String(data, Charsets.US_ASCII);
			}
			if (inInputStream.readableBytes() >= 2) {
				inInputStream.readBytes(data, 4);
				firmwareVersion = new String(data, Charsets.US_ASCII);
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

	public String getHardwareVersion() {
		return hardwareVersion;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public byte getRadioProtocolVersion() {
		return radioProtocolVersion;
	}

	public byte getSystemStatus() {
		return systemStatus;
	}

}
