/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandABC.java,v 1.1 2011/01/21 01:08:20 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;

// --------------------------------------------------------------------------
/**
 *  Commands are basic to both systems (toy and hand-held).  Some commands are meant to carry
 *  content that is specific to one or the other system.  Query and response commands, for example,
 *  carry queries and responses that are specific to the system.
 *  
 *  The design goal of the system is to have a command command processing structure for all activities.
 *  System-specific customization should occur at the command content level.
 *
 * @author jeffw
 */

public abstract class CommandABC implements ICommand {

	public static final String	COMMAND_ID_KEY	= "cmdID";

	private static final Log	LOGGER			= LogFactory.getLog(CommandABC.class);

	private CommandIdEnum		mCommandId;
	private NetworkId			mNetworkId;
	private NetAddress			mSrcAddr;
	private NetAddress			mDstAddr;
	private long				mScheduleTimeNanos;
	private long				mSentTimeMillis;
	private int					mSendCount;
	private byte				mAckId;
	private AckStateEnum		mAckState;
	private byte[]				mAckData;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a command that's coming off of the network.
	 */
	public CommandABC() {
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a command at the controller to send to the network.
	 *  @param inCommandID	The command type we're creating.
	 */
	public CommandABC(CommandIdEnum inCommandId) {
		mCommandId = inCommandId;
	}

	// --------------------------------------------------------------------------
	/**
	 *  The "do" method for reading the command off of the input stream.
	 *  @param inTransport	The transport to read.
	 */
	protected void doFromTransport(ITransport inTransport) {
		mCommandId = inTransport.getCommandId();
		mNetworkId = inTransport.getNetworkId();
		mSrcAddr = inTransport.getSrcAddr();
		mDstAddr = inTransport.getDstAddr();
	}

	// --------------------------------------------------------------------------
	/**
	 *  The "do" method for writing the command to the output stream.
	 *  @param inTransport	The output stream to write.
	 */
	protected void doToTransport(ITransport inTransport) {
		inTransport.setCommandId(mCommandId);
		inTransport.setNetworkId(mNetworkId);
		inTransport.setSrcAddr(mSrcAddr);
		inTransport.setDstAddr(mDstAddr);
	}

	// --------------------------------------------------------------------------
	/**
	 *  The "do" method that creates a debug string for the object.
	 *  @return	A debug string for the object.
	 */
	protected abstract String doToString();
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getCommandIdEnum()
	 */
	public CommandIdEnum getCommandIdEnum() {
		return mCommandId;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The command ID for this command.
	 */
	public final int getCommandIDAsInt() {
		return getCommandGroupEnum().getValue();
	}

	// --------------------------------------------------------------------------
	/**
	 *  The parent method that controls the reading of a command from the input stream.
	 *  @param inTransport	The transport to read.
	 */
	public void fromTransport(ITransport inTransport) {

		// Have the subclasses get their data from the input stream.
		doFromTransport(inTransport);
	}

	// --------------------------------------------------------------------------
	/**
	 *  The parent method that controls the writing of a command to the output stream.
	 *  @param inTransport	The transport to write.
	 */
	public void toTransport(ITransport inTransport) {

		// Have the subclasses put their data into the output stream.
		doToTransport(inTransport);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getAckId()
	 */
	public final byte getAckId() {
		return mAckId;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#setAckId(byte)
	 */
	public final void setAckId(byte inAckId) {
		mAckId = inAckId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.IPacket#getAckState()
	 */
	public final AckStateEnum getAckState() {
		return mAckState;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.IPacket#setAckedState(com.gadgetworks.codeshelf.command.AckedStateEnum)
	 */
	public final void setAckState(AckStateEnum inAckedState) {
		mAckState = inAckedState;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.IPacket#getAckData()
	 */
	public final byte[] getAckData() {
		return mAckData;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.IPacket#setAckData(int)
	 */
	public final void setAckData(byte[] inAckData) {
		mAckData = inAckData;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		String resultStr;

		resultStr = "  cmd id:" + getCommandIDAsInt();
		resultStr += " cmd=" + getCommandName();
		resultStr += " " + doToString();

		return resultStr;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return human-readable command name for this commane.
	 * @return	The command name.
	 */
	private String getCommandName() {
		return getCommandGroupEnum().getName();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getSendCount()
	 */
	public final int getSendCount() {
		return mSendCount;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#incrementSendCount()
	 */
	public final void incrementSendCount() {
		mSendCount++;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getSentTimeMillis()
	 */
	public final long getSentTimeMillis() {
		return mSentTimeMillis;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#setSentTimeMillis(long)
	 */
	public final void setSentTimeMillis(long inSentTimeMillis) {
		mSentTimeMillis = inSentTimeMillis;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getNetworkId()
	 */
	public final NetworkId getNetworkId() {
		return mNetworkId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#setNetworkId(com.gadgetworks.codeshelf.command.NetworkId)
	 */
	public final void setNetworkId(NetworkId inNetworkId) {
		mNetworkId = inNetworkId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getNetSrcAddress()
	 */
	public final NetAddress getSrcAddr() {
		return mSrcAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#setSrcAddr(com.gadgetworks.codeshelf.command.NetAddress)
	 */
	public final void setSrcAddr(NetAddress inSrcAddr) {
		mSrcAddr = inSrcAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getDstAddr()
	 */
	public final NetAddress getDstAddr() {
		return mDstAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#setDstAddr(com.gadgetworks.codeshelf.command.NetAddress)
	 */
	public final void setDstAddr(NetAddress inDstAddr) {
		mDstAddr = inDstAddr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#getScheduledTimeNanos()
	 */
	public final long getScheduledTimeNanos() {
		return mScheduleTimeNanos;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICommand#setScheduledTimeNanos(long)
	 */
	public final void setScheduledTimeNanos(long inSendTimeNanos) {
		mScheduleTimeNanos = inSendTimeNanos;
	}
}
