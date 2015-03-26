package com.codeshelf.ws.jetty.protocol.command;

import com.codeshelf.ws.jetty.protocol.request.PingRequest;
import com.codeshelf.ws.jetty.protocol.response.PingResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class PingCommand extends CommandABC {

	private PingRequest request;
	
	public PingCommand(WebSocketConnection connection, PingRequest pingRequest) {
		super(connection);
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
