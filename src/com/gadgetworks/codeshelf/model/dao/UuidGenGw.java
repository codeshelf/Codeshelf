/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UuidGenGw.java,v 1.1 2012/12/25 10:48:14 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.UUID;

import com.avaje.ebean.Transaction;
import com.avaje.ebean.config.dbplatform.IdGenerator;
import com.eaio.uuid.UUIDGen;
/**
 * @author jeffw
 *
 */
public class UuidGenGw implements IdGenerator {

	/**
	 * 
	 */
	public UuidGenGw() {

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.dbplatform.IdGenerator#getName()
	 */
	public final String getName() {
		return "UuidGenGw";
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.dbplatform.IdGenerator#isDbSequence()
	 */
	public final boolean isDbSequence() {
		return false;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.dbplatform.IdGenerator#nextId(com.avaje.ebean.Transaction)
	 */
	public final Object nextId(Transaction inTransaction) {
		return new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode());
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.dbplatform.IdGenerator#preAllocateIds(int)
	 */
	public final void preAllocateIds(int inAllocateSize) {
		// ignored
	}

}
