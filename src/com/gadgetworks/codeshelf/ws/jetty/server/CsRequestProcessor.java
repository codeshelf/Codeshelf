package com.gadgetworks.codeshelf.ws.jetty.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.command.CommandABC;
import com.gadgetworks.codeshelf.ws.jetty.command.EchoCommand;
import com.gadgetworks.codeshelf.ws.jetty.command.LoginCommand;
import com.gadgetworks.codeshelf.ws.jetty.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;

public class CsRequestProcessor implements RequestProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsRequestProcessor.class);

	@Override
	public ResponseABC handleRequest(CsSession session, RequestABC request) {
		LOGGER.info("Request received for processing: "+request);
		CommandABC command = null;
		ResponseABC response = null;

		// TODO: consider changing to declarative implementation using custom annotation
		// to get rid of handling via if statements and type casts...
		if (request instanceof LoginRequest) {
			LoginRequest loginRequest = (LoginRequest) request;
			LoginCommand loginCommand = new LoginCommand(session,loginRequest);
			command = (CommandABC) loginCommand;
		}
		else if (request instanceof EchoRequest) {
			EchoRequest echoRequest = (EchoRequest) request;
			EchoCommand echoCommand = new EchoCommand(session,echoRequest);
			command = (CommandABC) echoCommand;
		}		
		
		// check if matching command was found
		if (command==null) {
			LOGGER.warn("Unable to find matching command for request "+request+". Ignoring request.");
			return null;
		}

		// execute command and generate response to be sent to client
		response = command.exec();
		if (response!=null) {
			// automatically tie response to request
			response.setRequestID(request.getRequestId());
		}
		else {
			LOGGER.warn("No response generated for request "+request);
		}
		return response;
	}

}
