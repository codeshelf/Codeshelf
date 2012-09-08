/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: QueryFactoryABC.java,v 1.5 2012/09/08 03:03:22 jeffw Exp $
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
