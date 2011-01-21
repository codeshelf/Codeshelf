/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandInfoResponse.java,v 1.1 2011/01/21 01:08:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.query.IResponse;
import com.gadgetworks.codeshelf.query.IResponseFactory;
import com.gadgetworks.codeshelf.query.ResponseFactory;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *  The RESPONSE command is sent in response to a query from another device in the network.
 */
public final class CommandInfoResponse extends CommandInfoABC {

	public static final String	BEAN_ID	= "CommandInfoResponse";
	private static final Log	LOGGER	= LogFactory.getLog(CommandInfoResponse.class);

	private IResponse			mResponse;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a new response command to send to the network.
	 *  @param inResponseType	The response type sent.
	 *  @param inEndpoint	The endpoint to send the command to.
	 */
	public CommandInfoResponse(final IResponse inResponse) {
		super(CommandIdEnum.RESPONSE);

		mResponse = inResponse;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandInfoResponse() {
		super(CommandIdEnum.RESPONSE);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	@Override
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		try {
			//mResponse = ResponseFactoryABC.createResponse(inInputStream, inCommandByteCount);
			//IResponseFactory responseFactory = (IResponseFactory) FlyWeightBeanFactory.getBean(IResponseFactory.BEAN_ID);
			IResponseFactory responseFactory = new ResponseFactory();
			mResponse = responseFactory.createResponse(inTransport);
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
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	Return the response that is embedded in this command.
	 */
	public IResponse getResponse() {
		return mResponse;
	}

	/* --------------------------------------------------------------------------</p>
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doToString()
	 */
	@Override
	protected String doToString() {
		return mResponse.toString();
	}

}
