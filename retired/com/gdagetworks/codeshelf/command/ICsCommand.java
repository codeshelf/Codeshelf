/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsCommand.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.model.domain.PickTag;

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
