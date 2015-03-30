package com.codeshelf.ws.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.protocol.request.EchoRequest;
import com.codeshelf.ws.protocol.response.EchoResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.server.WebSocketConnection;

public class EchoCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(EchoCommand.class);
	EchoRequest mRequest;
	
	public EchoCommand(WebSocketConnection connection, EchoRequest request) {
		super(connection);
		mRequest = request;
	}

	@Override
	public ResponseABC exec() {
		LOGGER.info("Executing "+this);
		// send the same message back to the client
		return new EchoResponse(mRequest.getMessage());
	}
}
