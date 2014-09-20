package com.gadgetworks.codeshelf.ws.jetty.protocol.message;

import lombok.Setter;

import com.gadgetworks.codeshelf.ws.jetty.client.MessageCoordinator;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public abstract class MessageProcessor {
	
	@Setter
	MessageCoordinator messageCoordinator;
	
	public abstract ResponseABC handleRequest(CsSession session, RequestABC request);

	public abstract void handleResponse(CsSession session, ResponseABC response);

	public abstract void handleOtherMessage(CsSession session, MessageABC message);
}
