/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IAtopCommand.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

/**
 * @author jeffw
 *
 */
public interface IAtopCommand extends ICommand {
	
	// --------------------------------------------------------------------------
	/**
	 * Return true if this command expects a sub-node in the data stream.
	 * @return
	 */
	boolean hasSubNode();
	
	// --------------------------------------------------------------------------
	/**
	 * Get the sub-node for this command (if there is one).
	 * @return
	 */
	short getSubNode();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the subnode for this command.
	 * @param inSubNode
	 */
	void setSubNode(short inSubNode);
	
	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public byte[] getDataBytes();

	// --------------------------------------------------------------------------
	/**
	 * @param inDataBytes
	 */
	public void setDataBytes(byte[] inDataBytes);
	
}
