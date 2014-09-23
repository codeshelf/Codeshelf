package com.gadgetworks.codeshelf.ws.jetty.protocol.message;

import lombok.Setter;

import com.gadgetworks.codeshelf.ws.jetty.client.MessageCoordinator;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public abstract class MessageProcessor {
	
	@Setter
	MessageCoordinator messageCoordinator;
	
	public abstract ResponseABC handleRequest(UserSession session, RequestABC request);

	public abstract void handleResponse(UserSession session, ResponseABC response);

	public abstract void handleOtherMessage(UserSession session, MessageABC message);
}
