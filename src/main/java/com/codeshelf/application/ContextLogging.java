package com.codeshelf.application;

import org.apache.logging.log4j.ThreadContext;

import com.codeshelf.flyweight.command.NetGuid;

/*
 * static class for setting various ThreadContext values for logging.
 * 
 * note that authenticated user info is placed in context by CodeshelfSecurityManager instead
 */
public class ContextLogging {
	private static final String	NETGUID_KEY	= "netguid";

	private ContextLogging() {}
	////////////////////////////
		
	static public void setNetGuid(NetGuid guid) {
		setNetGuid(guid.getHexStringNoPrefix());
	}
	static public void setNetGuid(String guidStr) {
		ThreadContext.put(NETGUID_KEY, guidStr);
	}
	static public void clearNetGuid() {
		ThreadContext.remove(NETGUID_KEY);
	}

	public static String getNetGuid() {
		return ThreadContext.get(NETGUID_KEY);
	}

}
