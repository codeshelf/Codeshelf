/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionReqCmdFactory.java,v 1.1 2012/03/17 09:07:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public interface IWebSessionReqCmdFactory {

	IWebSessionReqCmd createWebSessionCommand(JsonNode inCommandAsJson);

}
