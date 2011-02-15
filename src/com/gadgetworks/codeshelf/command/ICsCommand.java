/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsCommand.java,v 1.2 2011/02/15 02:39:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public interface ICsCommand extends ICommand {

	// --------------------------------------------------------------------------
	/**
	 * Get the pick tag that goes with this command.
	 * @return
	 */
	PickTag getPickTag();
}
