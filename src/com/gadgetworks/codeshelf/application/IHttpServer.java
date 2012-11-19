/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IHttpServer.java,v 1.3 2012/11/19 10:48:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

/**
 * @author jeffw
 *
 */
public interface IHttpServer {

	String	WEBSITE_DEFAULT_PAGE_PROPERTY		= "WEBSITE_DEFAULT_PAGE";
	String	WEBSITE_CONTENT_PATH_PROPERTY		= "WEBSITE_CONTENT_PATH";
	String	WEBSITE_HOSTNAME_PROPERTY			= "WEBSITE_HOSTNAME";
	String	WEBSITE_PORTNUM_PROPERTY			= "WEBSITE_PORTNUM";
	int		WEBSITE_DEFAULT_PORTNUM				= 8000;

	String	WEBAPP_DEFAULT_PAGE_PROPERTY		= "WEBAPP_DEFAULT_PAGE";
	String	WEBAPP_CONTENT_PATH_PROPERTY		= "WEBAPP_CONTENT_PATH";
	String	WEBAPP_HOSTNAME_PROPERTY			= "WEBAPP_HOSTNAME";
	String	WEBAPP_PORTNUM_PROPERTY				= "WEBAPP_PORTNUM";
	int		WEBAPP_DEFAULT_PORTNUM				= 8443;

	String	KEYSTORE_TYPE_PROPERTY				= "KEYSTORE_TYPE_PROPERTY";
	String	KEYSTORE_PATH_PROPERTY				= "KEYSTORE_PATH_PROPERTY";
	String	KEYSTORE_STORE_PASSWORD_PROPERTY	= "KEYSTORE_STORE_PASSWORD_PROPERTY";
	String	KEYSTORE_KEY_PASSWORD_PROPERTY		= "KEYSTORE_KEY_PASSWORD_PROPERTY";

	void startServer();

	void stopServer();

}
