package com.codeshelf.ws.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.server.WebSocketConnection;

public abstract class CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER = LoggerFactory.getLogger(CommandABC.class);

	WebSocketConnection wsConnection;
	
	public CommandABC(WebSocketConnection connection) {
		this.wsConnection = connection;
	}
	
	public abstract ResponseABC exec();

}
