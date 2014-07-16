package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public abstract class CommandABC {
	
	@Getter @Setter
	CsSession session;
	
	public CommandABC(CsSession session) {
		this.session = session;
	}

	public abstract ResponseABC exec();
}
