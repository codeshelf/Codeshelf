package com.codeshelf.ws.protocol.command;

import com.codeshelf.ws.protocol.request.PingRequest;
import com.codeshelf.ws.protocol.response.PingResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

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
