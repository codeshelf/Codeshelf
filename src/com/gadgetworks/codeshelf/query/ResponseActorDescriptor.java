/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseActorDescriptor.java,v 1.3 2011/01/21 02:22:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetResponseTypeID;

//--------------------------------------------------------------------------
/**
 * A response to a query for general information about a remote actor device. 
 * 
 * The response format is:
 * 
 * 8B - The GUID for the remote device.
 * 1B - The device type for the remote device.
 * NB - The device description as a pascal-style string.
 * 1B - The number of KVPs for the remote device actor.
 * 1B - The number of endpoints for the remote device actor.
 * 
 * Where B = byte, b = bit.
 * 
 *  @author jeffw
 *  @see	QueryABC
 */
public final class ResponseActorDescriptor extends ResponseABC {

	public static final String	BEAN_ID				= "ResponseActorDescriptor";
	public static final byte	KVP_CNT_SIZE		= 1;
	public static final byte	ENDPOINT_CNT_SIZE	= 1;

	private byte[]				mGUID				= new byte[INetworkDevice.UNIQUEID_BYTES];
	private String				mDescStr;
	// These really are bytes on the stream, but Java doesn't allow unsigned bytes
	private short				mDeviceType;
	private short				mKVPCount;

	public ResponseActorDescriptor() {
		super();
	}

	public ResponseActorDescriptor(final byte[] inGUID,
		final String inDescStr,
		final short inDeviceType,
		final short inKVPCount,
		final short inEndpointCount) {
		mGUID = inGUID;
		mDescStr = inDescStr;
		mDeviceType = inDeviceType;
		mKVPCount = inKVPCount;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQueryTypeID()
	 */
	public ResponseTypeEnum getResponseType() {
		return ResponseTypeEnum.ACTOR_DESCRIPTOR;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.ResponseABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	@Override
	protected void doFromTransport(ITransport inTransport) throws IOException {
		inBitFieldInputStream.readBytes(mGUID, INetworkDevice.UNIQUEID_BYTES);
		mDeviceType = inBitFieldInputStream.readByte();
		mDescStr = inBitFieldInputStream.readPString();
		mKVPCount = inBitFieldInputStream.readByte();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.ResponseABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	@Override
	protected void doToTransport(ITransport inTransport) throws IOException {
		inBitFieldOutputStream.writeBytes(mGUID);
		inBitFieldOutputStream.writeByte((byte) mDeviceType);
		inBitFieldOutputStream.writePString(mDescStr);
		inBitFieldOutputStream.writeByte((byte) mKVPCount);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.ResponseABC#doToString()
	 */
	@Override
	protected String doToString() {
		return BEAN_ID + " GUID=" + new String(mGUID) + " desc=" + mDescStr + " kvpCnt=" + mKVPCount;
	}

	public String getGUIDStr() {
		return new String(mGUID);
	}

	public short getDeviceType() {
		return mDeviceType;
	}

	public String getDescStr() {
		return mDescStr;
	}

	public short getKVPCount() {
		return mKVPCount;
	}
}
