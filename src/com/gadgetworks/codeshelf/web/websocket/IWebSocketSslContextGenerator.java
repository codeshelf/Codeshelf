/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSocketSslContextGenerator.java,v 1.2 2012/11/19 10:48:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import javax.net.ssl.SSLContext;

public interface IWebSocketSslContextGenerator {

	String	KEYSTORE_TYPE_PROPERTY				= "KEYSTORE_TYPE_PROPERTY";
	String	KEYSTORE_PATH_PROPERTY				= "KEYSTORE_PATH_PROPERTY";
	String	KEYSTORE_STORE_PASSWORD_PROPERTY	= "KEYSTORE_STORE_PASSWORD_PROPERTY";
	String	KEYSTORE_KEY_PASSWORD_PROPERTY		= "KEYSTORE_KEY_PASSWORD_PROPERTY";

	SSLContext getSslContext();
}
