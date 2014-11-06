package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public abstract class CommandABC {
	private static final Logger	LOGGER = LoggerFactory.getLogger(CommandABC.class);

	UserSession session;
	
	public CommandABC(UserSession session) {
		this.session = session;
	}
	
	public abstract ResponseABC exec();

}
