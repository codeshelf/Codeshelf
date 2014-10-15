package com.gadgetworks.codeshelf.application;

import org.apache.log4j.MDC;

import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.flyweight.command.NetGuid;

public class ContextLogging {
	////////////////////////////
	
	static public void setSession(CsSession csSession) {
    	String username = "unknown";
    	if(csSession != null) {
        	User user = csSession.getUser();
        	if(user != null) {
        		username = user.getDomainId();
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
