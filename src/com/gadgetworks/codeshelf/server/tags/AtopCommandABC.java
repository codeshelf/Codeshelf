/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCommandABC.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

/**
 * @author jeffw
 *
 */
public abstract class AtopCommandABC implements IAtopCommand {

	private short	mMsgType;
	private short	mSubCommand;
	private short	mSubNode;
	private byte[]	mDataBytes;

	public AtopCommandABC(final short inMsgType, final short inSubCommand) {
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
	/**
	 * Return the command name.
	 * @return
	 */
	protected abstract String doGetCommandName();

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
	public String toString() {
		String result = "ATOP Msg:" + mMsgType + " SubCmd:" + doGetCommandName() + " SubNode:" + mSubNode;
		;

		return result;
	}
}
