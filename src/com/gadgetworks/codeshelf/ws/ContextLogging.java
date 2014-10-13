package com.gadgetworks.codeshelf.ws;

import org.apache.log4j.MDC;

import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class ContextLogging {
	
	static public String set(CsSession csSession) {
    	String username = "unknown";
    	if(csSession != null) {
        	User user = csSession.getUser();
        	if(user != null) {
        		username = user.getDomainId();
        	}
    	}
    	ContextLogging.setUsername(username);
    	return username;
	}
	
	static public void setUsername(String username) {
		MDC.put("username", username);
	}

	static public void clear() {
    	MDC.clear();
	}

}
