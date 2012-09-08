/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: QueryABC.java,v 1.5 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 * An implementation of the IQuery interface for general query behavior. 
 * 
 * The query format is:
 * 
 * 1B - Query type ID
 * 8B - The query's unique ID
 * NB - The query data
 * 
 * Where B = byte, b = bit.
 * 
 *  @author jeffw
 *  @see	IQuery
 */
public abstract class QueryABC implements IQuery {

	private static final Log	LOGGER			= LogFactory.getLog(QueryABC.class);

	private static final int	QUERY_HDR_SIZE	= 9;

	// The query ID is the ordinal millisecond that we sent the query.
	private long				mQueryTimeMillis;
	private INetworkDevice		mAssociatedDevice;
	private byte				mSendCount;

	public QueryABC() {
		mQueryTimeMillis = System.currentTimeMillis();
	}

	// --------------------------------------------------------------------------
	/**
	 *  Give the sub-classes a chance to output the class-specific query data to the output stream.
	 *  @param inTransport	The output transport to write.
	 */
	protected abstract void doToTransport(ITransport inTransport) throws IOException;

	// --------------------------------------------------------------------------
	/**
	 *  Give the sub-classes a chance to read the class-specific query data from the input stream.
	 *  @param inTransport	The input transport to read.
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
		return "Query: " + Long.toHexString(this.getQueryID()) + " " + this.doToString();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQueryTimeMillis()
	 */
	public final long getQueryTimeMillis() {
		return mQueryTimeMillis;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#setQueryTimeMillis(long)
	 */
	public final void setQueryTimeMillis(long inNewQueryTime) {
		mQueryTimeMillis = inNewQueryTime;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQueryID()
	 */
	public final long getQueryID() {
		return mQueryTimeMillis;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQuerySession()
	 */
	public final INetworkDevice getQueryNetworkDevice() {
		return mAssociatedDevice;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#setQuerySession(com.gadgetworks.controller.ISession)
	 */
	public final void setQueryNetworkDevice(INetworkDevice inNetworkDevice) {
		mAssociatedDevice = inNetworkDevice;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#toStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	public final void toTransport(ITransport inTransport) {

		try {
			// Output the query ID type code.
			inTransport.setNextParam(this.getQueryType());
			inTransport.setNextParam(mQueryTimeMillis);

			this.doToTransport(inTransport);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#fromStream(com.gadgetworks.bitfields.BitFieldInputStream)
	 */
	public final void fromTransport(ITransport inTransport) {

		try {
			mQueryTimeMillis = ((Long) inTransport.getParam(2)).longValue();

			this.doFromTransport(inTransport);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#incrementSendCount()
	 */
	public final void incrementSendCount() {
		mSendCount++;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getSendCount()
	 */
	public final byte getSendCount() {
		return mSendCount;
	}

}
