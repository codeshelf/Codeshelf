package com.codeshelf.application;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;

/*
 * static class for setting various ThreadContext values for logging.
 * 
 * note that authenticated user info is placed in context by CodeshelfSecurityManager instead
 * 
 * Correct use is at the absolute outer-most level, setNetGuid(), then in finally block, clearNetGuid
 * Most other use is code that may or may not already have the right netGuid set, or is about other devices as a side effect.
 * In those cases, we want to remberThenSetNetGuid(), and restoreNetGuid()
 */
public class ContextLogging {
	private static final Logger				LOGGER	= LoggerFactory.getLogger(ContextLogging.class);
	private static final String	NETGUID_KEY	= "netguid";

	private ContextLogging() {
	}

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

	/**
	 * Most callers should use rememberThenSetNetGuid(), and restoreNetGuid();
	 */
	static public String rememberThenSetNetGuid(String guidStr) {
		if (guidStr == null) {
			LOGGER.error("null input to rememberThenSetNetGuid", new RuntimeException()); // give the stack trace
			return null;
		}		
		String remember = getNetGuid();
		// This part may way over-report. Ok to remove, or change to debug. Initially, want to know how much it is happening.
		if (guidStr.equals(remember)) {
			LOGGER.warn("Unnecessary guid context manipulation?", new RuntimeException()); // give the stack trace
		}
		
		setNetGuid(guidStr);
		return remember;
	}
	
	static public String rememberThenSetNetGuid(NetGuid guid) {
		if (guid == null) {
			LOGGER.error("null input to rememberThenSetNetGuid", new RuntimeException()); // give the stack trace
			return null;
		}		
		String guidStr = guid.getHexStringNoPrefix();
		return rememberThenSetNetGuid(guidStr);
	}

	static public void restoreNetGuid(String guidStr) {
		if (guidStr == null || guidStr.isEmpty())
			clearNetGuid();
		else
			setNetGuid(guidStr);
	}

}
