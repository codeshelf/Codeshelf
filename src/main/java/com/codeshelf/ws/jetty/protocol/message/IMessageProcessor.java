package com.codeshelf.ws.jetty.protocol.message;

import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public interface IMessageProcessor {
	
//	@Setter
//	MessageCoordinator messageCoordinator;
	
	ResponseABC handleRequest(WebSocketConnection session, RequestABC request);

	void handleResponse(WebSocketConnection session, ResponseABC response);

	void handleMessage(WebSocketConnection session, MessageABC message);
}
