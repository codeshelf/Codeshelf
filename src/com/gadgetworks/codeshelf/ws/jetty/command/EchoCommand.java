package com.gadgetworks.codeshelf.ws.jetty.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.response.EchoResponse;
import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class EchoCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(EchoCommand.class);
	EchoRequest mRequest;
	
	public EchoCommand(CsSession session, EchoRequest request) {
		super(session);
		mRequest = request;
	}

	@Override
	public ResponseABC exec() {
		LOGGER.info("Executing "+this);
		// send the same message back to the client
		return new EchoResponse(mRequest.getMessage());
	}
}
