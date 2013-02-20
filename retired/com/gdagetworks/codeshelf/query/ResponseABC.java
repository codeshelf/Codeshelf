/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseABC.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;

//--------------------------------------------------------------------------
/**
 * An implementation of the IResponse interface for general response behavior. 
 * 
 * The response format is:
 * 
 * 1B - Response type ID
 * 4B - The originating query's unique ID
 * NB - The response data
 * 
 * Where B = byte, b = bit.
 * 
 *  @author jeffw
 *  @see	IResponse
 */
public abstract class ResponseABC implements IResponse {

	private static final Log	LOGGER				= LogFactory.getLog(ResponseABC.class);

	private static final int	RESPONSE_HDR_SIZE	= 9;

	private long				mQueryID;

	// --------------------------------------------------------------------------
	/**
	 *  Give the sub-classes a chance to output the class-specific query data to the output stream.
	 *  @param inBitFieldOutputStream	The output stream to write.
	 */
	protected abstract void doToTransport(ITransport inTransport) throws IOException;

	// --------------------------------------------------------------------------
	/**
	 *  Give the sub-classes a chance to read the class-specific query data from the input stream.
	 *  @param inBitFieldInputStream	The input stream to read.
	 *  @param inCommandByteCount	The number of bytes to read.
	 */
	protected abstract void doFromTransport(ITransport inTransport) throws IOException;

	// --------------------------------------------------------------------------
	/**
	 *  Give the sub-classes a chance to decorate the toString() return value.
	 *  @return	A string to append to the toString() return value.
	 */
	protected abstract String doToString();

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		return "Response: " + Long.toHexString(this.getQueryID()) + " " + this.doToString();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IResponse#toStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	public final void toTransport(ITransport inTransport) {

		try {
			// Output the query ID type code.
			inTransport.setNextParam(this.getResponseType());
			inTransport.setNextParam(mQueryID);

			this.doToTransport(inTransport);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IResponse#fromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	public final void fromTransport(ITransport inTransport) {

		try {
			mQueryID = ((Long) inTransport.getParam(1)).longValue();

			this.doFromTransport(inTransport);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IResponse#getQueryID()
	 */
	public final long getQueryID() {
		return mQueryID;
	}

	public final void setQueryID(long inQueryID) {
		mQueryID = inQueryID;
	}
}
