package com.gadgetworks.codeshelf.ws.jetty.server;

import javax.websocket.Session;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public interface ClientMessageProcessor {
	ResponseABC handleRequest(Session session, MessageABC message);
}
