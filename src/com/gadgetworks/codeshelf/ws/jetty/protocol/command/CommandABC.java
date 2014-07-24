package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public abstract class CommandABC {
	
	DaoProvider daoProvider;
	
	public CommandABC() {
	}

	public abstract ResponseABC exec();

	public void setDaoProvider(DaoProvider daoProvider) {
		this.daoProvider = daoProvider;
	}
}
