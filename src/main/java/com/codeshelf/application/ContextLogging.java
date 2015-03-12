package com.codeshelf.application;

import org.apache.logging.log4j.ThreadContext;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.manager.User;
import com.codeshelf.ws.jetty.server.UserSession;

public class ContextLogging {
	////////////////////////////
	
	static public void setSession(UserSession csSession) {
    	String username = "unknown";
    	if(csSession != null) {
        	User user = csSession.getUser();
        	if(user != null) {
        		username = user.getUsername();
        	}
    	}
    	ContextLogging.setUsername(username);
	}
	static public void clearSession() {
		clearUsername();
	}
	
	///////////////////////////
	
	static public void setNetGuid(NetGuid guid) {
		setNetGuid(guid.getHexStringNoPrefix());
	}
	static public void setNetGuid(String guidStr) {
		ThreadContext.put("netguid", guidStr);
	}
	static public void clearNetGuid() {
		ThreadContext.remove("netguid");
	}
	
	////////////////////////////
	
	static public void setUsername(String username) {
		ThreadContext.put("username", username);
	}	
	static public void clearUsername() {
		ThreadContext.remove("username");
	}

	//////////////////////////////
	
	static public void clearAll() {
		ThreadContext.clearAll();
	}

}
