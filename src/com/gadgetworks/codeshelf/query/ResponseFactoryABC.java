/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseFactoryABC.java,v 1.1 2011/01/21 01:08:16 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetResponseTypeID;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public abstract class ResponseFactoryABC implements IResponseFactory {

	protected abstract IResponse doCreateResponse(int inResponseTypeID);
	
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
		NetResponseTypeID responseTypeID;
		IResponse result = null;

		responseTypeID = inTransport.getParam(inParamKey);
		
		result = this.doCreateResponse(responseTypeID.getParamValue());
		
		if (result != null) {
			result.fromStream(inInputStream, inResponseByteCount);
		}

		return result;
	}

}
