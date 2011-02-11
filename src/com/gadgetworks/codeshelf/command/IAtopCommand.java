/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IAtopCommand.java,v 1.2 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.util.List;

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
	byte[] getDataBytes();

	// --------------------------------------------------------------------------
	/**
	 * @param inDataBytes
	 */
	void setDataBytes(byte[] inDataBytes);

	// --------------------------------------------------------------------------
	/**
	 * There is not a one-to-one mapping of inbound Atop commands to outbound CodeShelf commands.
	 * This method gets all of the outbound commands we must send in response to an Atop command.
	 * @return
	 */
	List<ICsCommand> setupOutboundCsCommands();
}
