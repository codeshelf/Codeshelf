/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsCommand.java,v 1.4 2012/09/08 03:03:22 jeffw Exp $
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
