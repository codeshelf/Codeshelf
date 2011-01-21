/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: QueryActorKVP.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

import java.io.IOException;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetQueryTypeId;

//--------------------------------------------------------------------------
/**
 * A query to request a KVP entry for the remote actor device. 
 * 
 * The query format is:
 * 
 * 1B - the KVP number requested
 * 
 * Where B = byte, b = bit.
 * 
 *  @author jeffw
 *  @see	QueryABC
 */
public final class QueryActorKVP extends QueryABC {

	public static final String	BEAN_ID			= "QueryActorKVP";
	public static final byte	KVP_NUMBER_SIZE	= 1;

	private byte				mKVPNumber;

	// --------------------------------------------------------------------------
	/**
	 */
	public QueryActorKVP(final byte inKVPNumber) {
		super();

		mKVPNumber = inKVPNumber;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getKVPNumber() {
		return mKVPNumber;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQueryTypeID()
	 */
	public NetQueryTypeId getQueryTypeID() {
		return new NetQueryTypeId(QueryTypeEnum.ACTOR_KVP.getValue());
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#getQuerySize()
	 */
	public int doGetQuerySize() {
		return KVP_NUMBER_SIZE;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#fromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	public void doFromTransport(ITransport inTransport) throws IOException {
		mKVPNumber = ((Byte) inTransport.getParam(1)).byteValue();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.IQuery#toStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	public void doToTransport(ITransport inTransport) throws IOException {
		inTransport.setParam(mKVPNumber, 1);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.QueryABC#doToString()
	 */
	public String doToString() {
		return BEAN_ID;
	}

}
