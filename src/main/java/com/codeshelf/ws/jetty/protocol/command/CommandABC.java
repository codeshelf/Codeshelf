package com.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public abstract class CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER = LoggerFactory.getLogger(CommandABC.class);

	WebSocketConnection session;
	
	public CommandABC(WebSocketConnection session) {
		this.session = session;
	}
	
	public abstract ResponseABC exec();

}
