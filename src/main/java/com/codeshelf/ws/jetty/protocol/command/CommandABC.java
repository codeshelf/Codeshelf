package com.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public abstract class CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER = LoggerFactory.getLogger(CommandABC.class);

	WebSocketConnection wsConnection;
	
	public CommandABC(WebSocketConnection connection) {
		this.wsConnection = connection;
	}
	
	protected Tenant getTenant() {
		if(wsConnection == null) 
			throw new RuntimeException("wsConnection was null for command that requires tenant context");
		User user = wsConnection.getUser();
		if(user == null) 
			throw new RuntimeException("wsConnection.getUser() was null for command that requires tenant context");
		Tenant tenant = user.getTenant();
		if(tenant == null) 
			throw new RuntimeException("wsConnection.getUser().getTenant() was null for command that requires tenant context");
		return tenant;
	}
	
	public abstract ResponseABC exec();

}
