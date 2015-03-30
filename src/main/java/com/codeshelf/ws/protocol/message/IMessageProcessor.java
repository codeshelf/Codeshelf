package com.codeshelf.ws.protocol.message;

import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.server.WebSocketConnection;

public interface IMessageProcessor {
	
//	@Setter
//	MessageCoordinator messageCoordinator;
	
	ResponseABC handleRequest(WebSocketConnection session, RequestABC request);

	void handleResponse(WebSocketConnection session, ResponseABC response);

	void handleMessage(WebSocketConnection session, MessageABC message);
}
