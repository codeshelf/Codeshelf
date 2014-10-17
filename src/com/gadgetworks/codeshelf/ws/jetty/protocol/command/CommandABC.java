package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public abstract class CommandABC {
	private static final Logger	LOGGER = LoggerFactory.getLogger(CommandABC.class);

	@Setter
	IDaoProvider daoProvider;
	
	UserSession session;
	
	public CommandABC(UserSession session) {
		this.session = session;
	}
	
	public CommandABC(IDaoProvider daoProvider, UserSession session) {
		this.daoProvider = daoProvider;
		this.session = session;
		if(daoProvider == null) {
			LOGGER.error("starting command with null daoProvider");
		}
		if(session == null) {
			LOGGER.error("starting command with null session");
		}
	}
	
	public abstract ResponseABC exec();

}
