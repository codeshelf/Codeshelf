/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: QueryFactoryABC.java,v 1.4 2011/02/20 00:18:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public abstract class QueryFactoryABC implements IQueryFactory {
	
	
	protected abstract IQuery doCreateQuery(int inQueryTypeID);
	
	// --------------------------------------------------------------------------
	/**
	 *  Read the next query off of the input stream.
	 *  @param inInputStream	The stream to read.
	 *  @param inQueryByteCount	The number of bytes to read.
	 *  @return	The query read from the stream.
	 *  @throws IOException
	 */
	public final IQuery createQuery(ITransport inTransport) throws IOException {

		// Read the Query ID from the input stream, and create the query for it.
		ResponseTypeEnum queryTypeId;
		IQuery result = null;

		queryTypeId = (ResponseTypeEnum) inTransport.getParam(1);
		
		result = this.doCreateQuery(queryTypeId.getValue());
		
		return result;
	}

}
