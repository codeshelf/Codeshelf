/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IResponseFactory.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IResponseFactory {

	String	BEAN_ID	= "ResponseFactory";

	IResponse createResponse(ITransport inTransport) throws IOException;

}
