/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlMessage.java,v 1.3 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import com.gadgetworks.codeshelf.model.ControlProtocolEnum;

/**
 * @author jeffw
 *
 */
public final class ControlMessage {
	
	private String mId;
	private ControlProtocolEnum mProtocol;
	private byte[] mDataBytes;

	public ControlMessage(String inId, ControlProtocolEnum inProtocol, byte[] inDataBytes) {
		mId = inId;
		mProtocol = inProtocol;
		mDataBytes = inDataBytes;
	}
	
	public String getId() {
		return mId;
	}
	
	public void setId(String inId) {
		mId = inId;
	}
	
	public ControlProtocolEnum getProtocol() {
		return mProtocol;
	}
	
	public void setProtocol(ControlProtocolEnum inProtocol) {
		mProtocol = inProtocol;
	}
	
	public byte[] getDataBytes() {
		return mDataBytes;
	}
	
	public void setDataBytes(byte[] inDataBytes) {
		mDataBytes = inDataBytes;
	}
}
