package com.codeshelf.ws.jetty.protocol.message;

import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.server.UserSession;

public interface IMessageProcessor {
	
//	@Setter
//	MessageCoordinator messageCoordinator;
	
	ResponseABC handleRequest(UserSession session, RequestABC request);

	void handleResponse(UserSession session, ResponseABC response);

	void handleMessage(UserSession session, MessageABC message);
}
