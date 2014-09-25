package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import com.gadgetworks.codeshelf.ws.jetty.protocol.request.PingRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.PingResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class PingCommand extends CommandABC {

	private PingRequest request;
	
	public PingCommand(UserSession session, PingRequest pingRequest) {
		super(session);
		this.request = pingRequest;
	}

	@Override
	public ResponseABC exec() {
		PingResponse response = new PingResponse();
		response.setStartTime(request.getStartTime());
		response.setStatus(ResponseStatus.Success);
		return response;
	}
}
