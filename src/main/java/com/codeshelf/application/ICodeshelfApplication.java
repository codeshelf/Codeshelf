/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICodeshelfApplication.java,v 1.4 2013/02/12 19:19:42 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.application;

/**
 * @author jeffw
 *
 */
public interface ICodeshelfApplication {

	void startApplication() throws Exception;

	void handleEvents();
}
