/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSocketSslContextGenerator.java,v 1.1 2012/11/15 07:55:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import javax.net.ssl.SSLContext;

public interface IWebSocketSslContextGenerator {

	SSLContext getSslContext();
}
