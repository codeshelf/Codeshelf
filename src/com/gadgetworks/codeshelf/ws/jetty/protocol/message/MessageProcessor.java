package com.gadgetworks.codeshelf.ws.jetty.protocol.message;

import javax.websocket.Session;

import lombok.Setter;

import com.gadgetworks.codeshelf.ws.jetty.client.MessageCoordinator;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public abstract class MessageProcessor {
	
	@Setter
	MessageCoordinator messageCoordinator;
	
	public abstract void handleResponse(Session session, ResponseABC response);

	public abstract ResponseABC handleRequest(Session session, RequestABC request);
}
