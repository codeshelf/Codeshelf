package com.codeshelf.application;

import org.apache.log4j.MDC;

import com.codeshelf.platform.multitenancy.User;
import com.codeshelf.ws.jetty.server.UserSession;
import com.codeshelf.flyweight.command.NetGuid;

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
		MDC.put("netguid", guidStr);
	}
	static public void clearNetGuid() {
		MDC.remove("netguid");
	}
	
	////////////////////////////
	
	static public void setUsername(String username) {
		MDC.put("username", username);
	}	
	static public void clearUsername() {
    	MDC.remove("username");
	}

	//////////////////////////////
	
	static public void clearAll() {
    	MDC.clear();
	}

}
