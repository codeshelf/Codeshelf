/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseActorKVP.java,v 1.3 2011/01/21 02:22:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetResponseTypeID;


//--------------------------------------------------------------------------
/**
 * A response to a query for KVP information for a remote actor device. 
 * 
 * The response format is:
 * 
 * 8B - The GUID for the remote device.
 * 1B - The KVP number responding.
 * NB - The key as a pascal-type string.
 * NB - the value as a pascal-type string.
 * 
 * Where B = byte, b = bit.
 * 
 *  @author jeffw
 *  @see	QueryABC
 */
public final class ResponseActorKVP extends ResponseABC {

	public static final String	BEAN_ID			= "ResponseActorKVP";
	public static final byte	KVPNUMBER_SIZE	= 1;

	private byte[]				mGUID			= new byte[INetworkDevice.UNIQUEID_BYTES];
	private byte				mKVPNumber;
	private String				mKeyStr;
	private String				mValueStr;

	public ResponseActorKVP() {
		super();
	}

	public ResponseActorKVP(byte[] inGUID, byte inKVPNumber, String inKeyStr, String inValueStr) {
		mGUID = inGUID;
		mKVPNumber = inKVPNumber;
		mKeyStr = inKeyStr;
		mValueStr = inValueStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQueryTypeID()
	 */
	public ResponseTypeEnum getResponseType() {
		return ResponseTypeEnum.ACTOR_KVP;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.query.ResponseABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	@Override
	protected void doFromTransport(ITransport inTransport) throws IOException {
		inBitFieldInputStream.readBytes(mGUID, INetworkDevice.UNIQUEID_BYTES);
		mKVPNumber = inBitFieldInputStream.readByte();
		mKeyStr = inBitFieldInputStream.readPString();
		mValueStr = inBitFieldInputStream.readPString();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.query.ResponseABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	@Override
	protected void doToTransport(ITransport inTransport) throws IOException {
		inBitFieldOutputStream.writeBytes(mGUID);
		inBitFieldOutputStream.writeByte(mKVPNumber);
		inBitFieldOutputStream.writePString(mKeyStr);
		inBitFieldOutputStream.writePString(mValueStr);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.ResponseABC#doToString()
	 */
	@Override
	protected String doToString() {
		return BEAN_ID + " key=" + mKeyStr + " value=" + mValueStr;
	}

	public String getKeyStr() {
		return mKeyStr;
	}

	public String getValueStr() {
		return mValueStr;
	}

	public byte getKVPNumber() {
		return mKVPNumber;
	}

}
