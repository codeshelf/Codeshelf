/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: QueryActorDescriptor.java,v 1.1 2011/01/21 01:08:16 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetQueryTypeId;

//--------------------------------------------------------------------------
/**
 * A query to request general information about a remote actor device. 
 * 
 * The query format is:
 * 
 * 0B - no bytes are sent in the body of this query
 * 
 * Where B = byte, b = bit.
 * 
 *  @author jeffw
 *  @see	QueryABC
 */
public final class QueryActorDescriptor extends QueryABC {

	public static final String	BEAN_ID	= "QueryActorDescriptor";

	// --------------------------------------------------------------------------
	/**
	 */
	public QueryActorDescriptor() {
		super();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQueryTypeID()
	 */
	public NetQueryTypeId getQueryTypeID() {
		return new NetQueryTypeId(QueryTypeEnum.ACTOR_DESCRIPTOR.getValue());
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#fromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	public void doFromTransport(ITransport inTransport) throws IOException {
		// Nothing gets read at this level.
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#toStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	public void doToTransport(ITransport inTransport) throws IOException {
		// Nothing gets written at this level.
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.QueryABC#doToString()
	 */
	public String doToString() {
		return BEAN_ID;
	}

}
