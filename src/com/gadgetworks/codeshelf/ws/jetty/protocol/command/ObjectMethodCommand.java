package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class ObjectMethodCommand extends CommandABC {

	private ObjectMethodRequest	request;

	public ObjectMethodCommand(ObjectMethodRequest request) {
		this.request = request;
	}
	
	@Override
	public ResponseABC exec() {
		ObjectMethodResponse response = new ObjectMethodResponse();
		// ...
		return response;
	}

}
