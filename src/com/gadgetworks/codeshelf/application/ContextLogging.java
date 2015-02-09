package com.gadgetworks.codeshelf.application;

import org.apache.log4j.MDC;

import com.gadgetworks.codeshelf.platform.multitenancy.User;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.gadgetworks.flyweight.command.NetGuid;

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
