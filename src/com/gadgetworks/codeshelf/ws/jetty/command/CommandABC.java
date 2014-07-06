package com.gadgetworks.codeshelf.ws.jetty.command;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public abstract class CommandABC {
	
	@Getter @Setter
	CsSession session;
	public CommandABC(CsSession session) {
		this.session = session;
	}

	public abstract ResponseABC exec();
}
