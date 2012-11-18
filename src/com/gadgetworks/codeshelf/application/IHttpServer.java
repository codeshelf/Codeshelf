/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IHttpServer.java,v 1.2 2012/11/18 06:04:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

/**
 * @author jeffw
 *
 */
public interface IHttpServer {

	String	WEBSITE_DEFAULT_PAGE	= "WEBSITE_DEFAULT_PAGE";
	String	WEBSITE_CONTENT_PATH	= "WEBSITE_CONTENT_PATH";
	String	WEBSITE_HOSTNAME		= "WEBSITE_HOSTNAME";
	String	WEBSITE_PORTNUM			= "WEBSITE_PORTNUM";
	int		WEBSITE_DEFAULT_PORTNUM	= 8000;

	String	WEBAPP_DEFAULT_PAGE		= "WEBAPP_DEFAULT_PAGE";
	String	WEBAPP_CONTENT_PATH		= "WEBAPP_CONTENT_PATH";
	String	WEBAPP_HOSTNAME			= "WEBAPP_HOSTNAME";
	String	WEBAPP_PORTNUM			= "WEBAPP_PORTNUM";
	int		WEBAPP_DEFAULT_PORTNUM	= 8443;

	void startServer();

	void stopServer();

}
