/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopABC.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;

/**
 * @author jeffw
 *
 */
public abstract class CommandAtopABC extends CommandABC implements IAtopCommand {

	private short	mMsgType;
	private short	mSubCommand;
	private short	mSubNode;
	private byte[]	mDataBytes;

	public CommandAtopABC(final CommandIdEnum inCommandIdEnum, final short inMsgType, final short inSubCommand) {
		super(inCommandIdEnum);
		mMsgType = inMsgType;
		mSubCommand = inSubCommand;
	}

	// --------------------------------------------------------------------------
	/**
	 * The subclasses tell us if they use subnode or not.
	 * @return
	 */
	protected abstract boolean doHasSubNode();

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected void doToTransport(ITransport inTransport) {
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected void doFromTransport(ITransport inTransport) {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandGroupEnum() {
		return CommandGroupEnum.ATOP;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final boolean hasSubNode() {
		return doHasSubNode();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.IAtopCommand#getSubNode()
	 */
	public final short getSubNode() {
		return mSubNode;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.IAtopCommand#setSubNode(short)
	 */
	public final void setSubNode(short inSubNode) {
		mSubNode = inSubNode;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.IAtopCommand#getDataBytes()
	 */
	public final byte[] getDataBytes() {
		return mDataBytes;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.IAtopCommand#setDataBytes(byte[])
	 */
	public final void setDataBytes(byte[] inDataBytes) {
		mDataBytes = inDataBytes;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String doToString() {
		CommandIdEnum cmdId = getCommandIdEnum();
		String result = "MsgType:" + Integer.toHexString(mMsgType) + " SubCmd:" + cmdId.toString() + " SubNode:" + mSubNode;
		return result;
	}
}
