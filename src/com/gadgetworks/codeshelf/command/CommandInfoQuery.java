/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandInfoQuery.java,v 1.3 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.query.IQuery;
import com.gadgetworks.codeshelf.query.IQueryFactory;
import com.gadgetworks.codeshelf.query.QueryFactory;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *  The QUERY command contains a request for information from another device on the network.
 */
public final class CommandInfoQuery extends CommandInfoABC {

	public static final String	BEAN_ID	= "CommandInfoQuery";
	private static final Log	LOGGER	= LogFactory.getLog(CommandInfoQuery.class);

	private IQuery				mQuery;

	// --------------------------------------------------------------------------
	/**
	 *  The is the constructor to use to create a new query command to send to the network.
	 *
	 *  Sometimes we need to resend queries (if they time-out), so we use this caching
	 *  mechanism to remember where we'd sent the query command, so that we can resend it.
	 *
	 *  @param inQuery	The query that the command will contain.
	 *  @param inDestAddr	The destination address of the query.
	 *  @param inEndpoint	The endpoint to send the command to.
	 */
	public CommandInfoQuery(final IQuery inQuery) {
		super(CommandIdEnum.QUERY);

		mQuery = inQuery;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandInfoQuery() {
		super(CommandIdEnum.QUERY);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Return the query associated with this query command.
	 *  @return	The query
	 */
	public IQuery getQuery() {
		return mQuery;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	@Override
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		try {
			//IQueryFactory queryFactory = (IQueryFactory) FlyWeightBeanFactory.getBean(IQueryFactory.BEAN_ID);
			IQueryFactory queryFactory = new QueryFactory();
			mQuery = queryFactory.createQuery(inTransport);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	@Override
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);

		mQuery.toTransport(inTransport);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doToString()
	 */
	@Override
	protected String doToString() {
		if (mQuery != null) {
			return mQuery.toString();
		} else {
			return "";
		}
	}

}
