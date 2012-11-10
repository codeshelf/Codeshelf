package com.gadgetworks.codeshelf.web.websocket;

import org.java_websocket.WebSocketListener;

public interface IGwWebSocketListener extends WebSocketListener {

	void start();
	
	void stop();

}
