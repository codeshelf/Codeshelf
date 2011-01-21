/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IResponse.java,v 1.1 2011/01/21 01:08:16 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetResponseTypeID;

//--------------------------------------------------------------------------
/**
 * The response format is:
 * 
 * 1B - Response type ID
 * 4B - The original query's unique ID
 * NB - The rest of the response's data
 * 
 * Where B = byte, b = bit.
 *  @author jeffw
 */

public interface IResponse {

	long getQueryID();
	
	void setQueryID(long inQueryID);

	NetResponseTypeID getResponseTypeID();

	void toTransport(ITransport inTransport);

	void fromTransport(ITransport inTransport);

	String toString();

}
