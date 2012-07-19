/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsCommand.java,v 1.3 2012/07/19 06:11:33 jeffw Exp $
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
