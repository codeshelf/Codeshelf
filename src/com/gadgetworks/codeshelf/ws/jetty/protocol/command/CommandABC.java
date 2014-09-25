package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import lombok.Setter;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public abstract class CommandABC {
	
	@Setter
	IDaoProvider daoProvider;
	
	UserSession session;
	
	public CommandABC(UserSession session) {
		this.session = session;
	}
	
	public CommandABC(IDaoProvider daoProvider, UserSession session) {
		this.daoProvider = daoProvider;
		this.session = session;
	}
	
	public abstract ResponseABC exec();

}
