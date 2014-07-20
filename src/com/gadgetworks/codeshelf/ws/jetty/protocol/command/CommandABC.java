package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public abstract class CommandABC {
	
	public CommandABC() {
	}

	public abstract ResponseABC exec();
}
