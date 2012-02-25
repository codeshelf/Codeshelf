/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCmdObjectGetterReq.java,v 1.2 2012/02/25 19:49:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.avaje.ebean.Ebean;
import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class WebSessionCmdObjectGetterReq extends WebSessionCmdABC {

	private static final Log	LOGGER				= LogFactory.getLog(WebSessionCmdObjectGetterReq.class);

	private static final String	CLASS_NODE			= "class";
	private static final String	ID_NODE				= "persistentId";
	private static final String	GETTER_METHOD		= "getterMethod";
	//	private static final String	QUERY_NODE	= "query";
	private static final String	OBJECT_RESULTS_NODE	= "result";

	private String				mParentId;
	private String				mParentClass;
	private String				mGetterMethodName;

	//	private String				mQuery;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionCmdObjectGetterReq(final String inCommandId, final JsonNode inDataNodeAsJson) {
		super(inCommandId, inDataNodeAsJson);

		JsonNode dataJsonNode = getDataJsonNode();
		JsonNode parentClassNode = dataJsonNode.get(CLASS_NODE);
		mParentClass = parentClassNode.getTextValue();
		JsonNode parentIdNode = dataJsonNode.get(ID_NODE);
		mParentId = String.valueOf(parentIdNode.getIntValue());
		JsonNode getMethodNode = dataJsonNode.get(GETTER_METHOD);
		mGetterMethodName = getMethodNode.getTextValue();
	}

	public final WebSessionCmdEnum getCommandEnum() {
		return WebSessionCmdEnum.LAUNCH_CODE_CHECK;
	}

	public final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {
			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(mParentClass);
			if (PersistABC.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				//				Class<?> clazz = classObject.getClass();
				Object parentObject = Ebean.find(classObject, mParentId);

				//				Query<? extends PersistABC> query = Ebean.find(clazz);
				//				//query.where(mQuery);
				//				//List<? extends PersistABC> resultsList = query.findList();

				// Execute the "get" method against the parents to return the children.
				// (The method *must* start with "get" to ensure other methods don't get called.)
				if (mGetterMethodName.startsWith("get")) {
					java.lang.reflect.Method method = parentObject.getClass().getMethod(mGetterMethodName, (Class<?>[]) null);
					Object resultObject = method.invoke(parentObject, (Object[]) null);

					// Convert the list of ojects into a JSon object.
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					ArrayNode searchListNode = mapper.valueToTree(resultObject);
					dataNode.put(OBJECT_RESULTS_NODE, searchListNode);

					result = new WebSessionCmdObjectGetterResp(dataNode);
				}
			}

		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SecurityException e) {
			LOGGER.error("", e);
		} catch (NoSuchMethodException e) {
			LOGGER.error("", e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	protected final void prepareDataNode(ObjectNode inOutDataNode) {
		// TODO Auto-generated method stub
	}
}
