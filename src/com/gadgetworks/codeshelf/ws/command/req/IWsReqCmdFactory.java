/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWsReqCmdFactory.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author jeffw
 *
 */
public interface IWsReqCmdFactory {

	IWsReqCmd createWebSessionCommand(JsonNode inCommandAsJson);

}
