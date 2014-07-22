package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Date;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

public class CsSession {

	@Getter @Setter
	String sessionId;
	
	@Getter
	Date sessionStart = new Date();
	
	@Getter @Setter
	boolean isAuthenticated = false;

	@Getter @Setter
	SessionType type = SessionType.Undefined;

	@Getter
	private Session	session=null;
	
	@Getter @Setter
	long lastPongReceived = 0;
	
	public CsSession(Session session) {
		this.session = session;
	}
}
