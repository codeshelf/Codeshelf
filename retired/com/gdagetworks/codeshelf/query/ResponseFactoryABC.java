/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseFactoryABC.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public abstract class ResponseFactoryABC implements IResponseFactory {

	protected abstract IResponse doCreateResponse(ResponseTypeEnum inResponseType);
	
	// --------------------------------------------------------------------------
	/**
	 *  Read the next response off of the input stream.
	 *  @param inInputStream	The stream to read.
	 *  @param inResponseByteCount	The number of bytes to read.
	 *  @return	The response read from the stream.
	 *  @throws IOException
	 */
	public final IResponse createResponse(ITransport inTransport) throws IOException {

		// Read the Response ID from the input stream, and create the Response for it.
		ResponseTypeEnum responseTypeId;
		IResponse result = null;

		responseTypeId = (ResponseTypeEnum) inTransport.getParam(1);
		
		result = this.doCreateResponse(responseTypeId);
		
		if (result != null) {
			result.fromTransport(inTransport);
		}

		return result;
	}

}
