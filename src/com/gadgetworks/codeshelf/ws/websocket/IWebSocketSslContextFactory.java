/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: file.java,v 1.1 2010/09/28 05:41:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

import javax.net.ssl.SSLContext;

/**
 * @author jeffw
 *
 */
public interface IWebSocketSslContextFactory {
	
	SSLContext getSslContext();

}
