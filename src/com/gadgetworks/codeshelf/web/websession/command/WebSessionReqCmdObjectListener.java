/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectListener.java,v 1.1 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectListener extends WebSessionReqCmdABC {

	private static final Log	LOGGER				= LogFactory.getLog(WebSessionReqCmdObjectListener.class);

	private static final String	OBJECT_CLASS		= "class";
	private static final String	OBJECT_ID_LIST		= "objectIds";
	private static final String	PROPERTY_NAME_LIST	= "propertyNames";
	private static final String	OBJECT_RESULTS_NODE	= "results";

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectListener(final String inCommandId, final JsonNode inDataNodeAsJson) {
		super(inCommandId, inDataNodeAsJson);
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.OBJECT_LISTENER_REQ;
	}

	public final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		// CRITICAL SECURITYY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {
			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode objectClassNode = dataJsonNode.get(OBJECT_CLASS);
			String mParentClass = objectClassNode.getTextValue();
			JsonNode objectIdListNode = dataJsonNode.get(OBJECT_ID_LIST);
			ObjectMapper mapper = new ObjectMapper();
			List<Long> mObjectIdList = mapper.readValue(objectIdListNode, new TypeReference<List<Long>>() {
			});
			JsonNode propertyNamesNode = dataJsonNode.get(PROPERTY_NAME_LIST);
			List<String> mPropertyNames = mapper.readValue(propertyNamesNode, new TypeReference<List<String>>() {
			});

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(mParentClass);
			if (PersistABC.class.isAssignableFrom(classObject)) {
				@SuppressWarnings("unchecked")
				// The above is a safety check not known to FindBugs or CheckStyle.
				Class<PersistABC> persistenceClass = (Class<PersistABC>) classObject;

				Query<PersistABC> query = Ebean.find(persistenceClass);
				List<PersistABC> methodResultsList = query.where().in("persistentId", mObjectIdList).findList();

				List<Map<String, String>> resultsList = new ArrayList<Map<String, String>>();
				for (PersistABC matchedObject : methodResultsList) {
					Map<String, String> propertiesMap = new HashMap<String, String>();
					propertiesMap.put("persistentId", matchedObject.getPersistentId().toString());
					for (String propertyName : mPropertyNames) {
						// Execute the "get" method against the parents to return the children.
						// (The method *must* start with "get" to ensure other methods don't get called.)
						String getterName = "get" + propertyName;
						java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
						Object resultObject = method.invoke(matchedObject, (Object[]) null);
						propertiesMap.put(propertyName, resultObject.toString());
					}
					resultsList.add(propertiesMap);
				}

				// Convert the list of objects into a JSon object.
				mapper = new ObjectMapper();
				ObjectNode dataNode = mapper.createObjectNode();
				ArrayNode searchListNode = mapper.valueToTree(resultsList);
				dataNode.put(OBJECT_RESULTS_NODE, searchListNode);

				result = new WebSessionRespCmdObjectListener(dataNode);
			}

		} catch (IOException e) {
			LOGGER.error("", e);
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
}
