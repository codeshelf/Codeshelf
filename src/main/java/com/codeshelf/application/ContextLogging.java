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

	public static final String				THREAD_CONTEXT_WORKER_KEY			= "worker";
	public static final String				THREAD_CONTEXT_TAGS_KEY				= "tags";
	public static final String				THREAD_CONTEXT_NETGUID_KEY			= "netguid";
	public static final String				THREAD_CONTEXT_USER_KEY				= "user";
	public static final String				THREAD_CONTEXT_TENANT_KEY			= "tenant";
	public static final String				THREAD_CONTEXT_FACILITY_KEY			= "facility";
	
	private ContextLogging() {
	}

	////////////////////////////

	static public void setNetGuid(NetGuid guid) {
		setNetGuid(guid.getHexStringNoPrefix());
	}

	static public void setNetGuid(String guidStr) {
		ThreadContext.put(THREAD_CONTEXT_NETGUID_KEY, guidStr);
	}

	static public void clearNetGuid() {
		ThreadContext.remove(THREAD_CONTEXT_NETGUID_KEY);
	}

	public static String getNetGuid() {
		return ThreadContext.get(THREAD_CONTEXT_NETGUID_KEY);
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
	
	static public String getTag(String tagId) {
		return ThreadContext.get(tagId);
	}
	
	static public void clearTag(String tagId) {
		ThreadContext.remove(tagId);
	}
	
	static public void setTag(String tagId, String tagValue) {
		if (tagValue == null || tagValue.isEmpty()){
			ThreadContext.remove(tagId);
		} else {
			ThreadContext.put(tagId, tagValue);
		}
	}
}
