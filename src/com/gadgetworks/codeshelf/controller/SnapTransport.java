/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: SnapTransport.java,v 1.6 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import java.util.ArrayList;
import java.util.List;

import com.gadgetworks.codeshelf.command.CommandIdEnum;

/**
 * @author jeffw
 *
 */
public final class SnapTransport implements ITransport {

	private CommandIdEnum	mCommandId;
	private NetworkId		mNetworkId;
	private NetAddress		mSrcAddr;
	private NetAddress		mDstAddr;
	private List<Object>	mParams;

	public SnapTransport() {
		mParams = new ArrayList<Object>();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#getNetworkId()
	 */
	public NetworkId getNetworkId() {
		return mNetworkId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#setNetworkId(com.gadgetworks.codeshelf.controller.NetworkId)
	 */
	public void setNetworkId(NetworkId inNetworkid) {
		mNetworkId = inNetworkid;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#getSrcAddr()
	 */
	public NetAddress getSrcAddr() {
		return mSrcAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#setSrcAddr(com.gadgetworks.codeshelf.controller.NetAddress)
	 */
	public void setSrcAddr(NetAddress inSrcAddr) {
		mSrcAddr = inSrcAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#getDstAddr()
	 */
	public NetAddress getDstAddr() {
		return mDstAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#setDstAddr(com.gadgetworks.codeshelf.controller.NetAddress)
	 */
	public void setDstAddr(NetAddress inDstAddr) {
		mDstAddr = inDstAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#getCommandId()
	 */
	public CommandIdEnum getCommandId() {
		return mCommandId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#setCommandId(com.gadgetworks.codeshelf.command.CommandIdEnum)
	 */
	public void setCommandId(CommandIdEnum inCommandId) {
		mCommandId = inCommandId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#setParam(java.lang.Object, int)
	 */
	public void setNextParam(Object inParam) {
		mParams.add(inParam);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#getParam(int)
	 */
	public Object getParam(int inParamNum) {
		return mParams.get(inParamNum);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.ITransport#getParams()
	 */
	public List<Object> getParams() {
		return mParams;
	}

}
