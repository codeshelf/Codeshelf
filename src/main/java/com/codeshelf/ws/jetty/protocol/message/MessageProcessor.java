package com.codeshelf.ws.jetty.protocol.message;

import lombok.Setter;

import com.codeshelf.ws.jetty.client.MessageCoordinator;
import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.server.UserSession;

public abstract class MessageProcessor {
	
	@Setter
	MessageCoordinator messageCoordinator;
	
	public abstract ResponseABC handleRequest(UserSession session, RequestABC request);

	public abstract void handleResponse(UserSession session, ResponseABC response);

	public abstract void handleMessage(UserSession session, MessageABC message);
}
