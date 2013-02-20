/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IQuery.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetQueryTypeId;

// --------------------------------------------------------------------------
/**
 * Queries and responses are very specific to the system-type.  e.g. Toy or terminal.  However,
 * there is some general behavior in the query/response mechanism shared by both systems.
 * 
 *  @author jeffw
 */

public interface IQuery {

	// --------------------------------------------------------------------------
	/**
	 *  Get the query's unique ID.
	 *  @return	The query's unique ID
	 */
	long getQueryID();
	
	// --------------------------------------------------------------------------
	/**
	 *  Get the time that the query was generated.
	 *  @return	The query time.
	 */
	long getQueryTimeMillis();
	
	// --------------------------------------------------------------------------
	/**
	 *  Set the time that the query was generated.
	 *  param	The new query time.
	 */
	void setQueryTimeMillis(long inNewQueryTime);
	
	// --------------------------------------------------------------------------
	/**
	 *  Get the query type.
	 *  @return	The query type.
	 */
	QueryTypeEnum getQueryType();
	
	// --------------------------------------------------------------------------
	/**
	 *  Get the session associated with this query.
	 *  @return	The associated session.
	 */
	INetworkDevice getQueryNetworkDevice();
	
	// --------------------------------------------------------------------------
	/**
	 *  Set the session associated with this query.
	 *  @param inSession	The associated session to set.
	 */
	void setQueryNetworkDevice(INetworkDevice inNetworkDevice);
	
	// --------------------------------------------------------------------------
	/**
	 *  Spool the query onto the output transport.
	 *  @param inTransport	The output transport to write the query.
	 */
	void toTransport(ITransport inTransport);

	// --------------------------------------------------------------------------
	/**
	 *  Get the query from the input transport.
	 *  @param inTransport	The input transport to read.
	 */
	void fromTransport(ITransport inTransport);
	
	// --------------------------------------------------------------------------
	/**
	 *  Increment the query send count each time we send it.
	 */
	void incrementSendCount();
	
	// --------------------------------------------------------------------------
	/**
	 *  Report how many times we've sent a query.
	 *  @return	The number of times we've sent the query.
	 */
	byte getSendCount();
	
	// --------------------------------------------------------------------------
	/**
	 *  Output a user-readable string of the query.
	 *  @return	The user-readable version of the query.
	 */
	String toString();

}
