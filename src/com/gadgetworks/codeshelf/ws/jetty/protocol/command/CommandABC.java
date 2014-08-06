package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public abstract class CommandABC {
	
	DaoProvider daoProvider;
	
	CsSession session;
	
	public CommandABC(CsSession session) {
		this.session = session;
	}

	public abstract ResponseABC exec();

	public void setDaoProvider(DaoProvider daoProvider) {
		this.daoProvider = daoProvider;
	}
}
