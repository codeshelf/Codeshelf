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
	
	public static final String				TAG_CHE_WORK_INSTRUCTION			= "CHE_EVENT Work_Instruction";
	public static final String				TAG_CHE_ORDER_INTO_WALL				= "CHE_EVENT Order_Into_Wall";
	public static final String				TAG_CHE_REMOVE_ORDER_CHE			= "CHE_EVENT Remove_Order_Che";
	public static final String				TAG_CHE_WORKER_ACTION				= "CHE_EVENT Worker Action";
	public static final String				TAG_CHE_WALL_PLANS_RESPONSE			= "CHE_EVENT Wall_Plans_Response";
	public static final String				TAG_CHE_WALL_PLANS_REQUEST			= "CHE_EVENT Wall_Plans_Request";
	public static final String				TAG_CHE_INVENTORY_UPDATE			= "CHE_EVENT Inventory_Update";
	public static final String				TAG_CHE_BUTTON						= "CHE_EVENT Button";
	public static final String				TAG_CHE_WALL_BUTTON_PRESS			= "CHE_EVENT Wall_Button_Press";
	public static final String				TAG_CHE_WALL_BUTTON_DISPLAY			= "CHE_EVENT Wall_Button_Display";
	public static final String				TAG_CHE_SCAN						= "CHE_EVENT Scan";
	public static final String				TAG_CHE_INFORMATION					= "CHE_EVENT Information";
	public static final String				TAG_CHE_ASSOCIATE					= "CHE_EVENT Associate";
	
	
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
	
	static public void setTenantAndFacility(String tenantId, String facilityId) {
		ThreadContext.put(ContextLogging.THREAD_CONTEXT_TENANT_KEY, tenantId);
		ThreadContext.put(ContextLogging.THREAD_CONTEXT_FACILITY_KEY, facilityId);
	}
}
