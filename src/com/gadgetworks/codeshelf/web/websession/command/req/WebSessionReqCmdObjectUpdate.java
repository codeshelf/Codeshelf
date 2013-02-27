/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectUpdate.java,v 1.18 2013/02/27 01:17:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectUpdate;

/**
 * 
 * INBOUND COMMAND STRUCTURE:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_UPDATE_REQ,
 * 	data {
 *		className:			<class_name>,
 *		parentPeristentID:	<id>,
 *		propertiesToSet [
 *			propertyName:	<propertyValue>
 *		]
 * 	}
 * }
 * 
 * OUTBOUND COMMAND STRUCTURE:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_UPDATE_RESP,
 * 	data {
 * 		newPersistentID: <id>
 *  }
 * }
 *
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectUpdate extends WebSessionReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WebSessionReqCmdObjectUpdate.class);

	private IDaoProvider		mDaoProvider;
	private Map<String, Object>	mUpdateProperties;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectUpdate(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mUpdateProperties = new HashMap<String, Object>();
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.OBJECT_UPDATE_REQ;
	}

	public final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {

			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode classNode = dataJsonNode.get(CLASSNAME);
			String className = classNode.getTextValue();
			if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				className = "com.gadgetworks.codeshelf.model.domain." + className;
			}
			JsonNode idNode = dataJsonNode.get(PERSISTENT_ID);
			UUID objectId = UUID.fromString(idNode.getTextValue());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode propertiesNode = dataJsonNode.get(PROPERTIES);
			List<Map<String, Object>> objectArray = mapper.readValue(propertiesNode, new TypeReference<List<Map<String, Object>>>() {
			});
			for (Map<String, Object> map : objectArray) {
				String name = (String) map.get("name");
				Object value = map.get("value");
				mUpdateProperties.put(name, value);
			}

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				IDomainObject updateObject = dao.findByPersistentId(objectId);

				// Execute the "set" method against the parents to return the children.
				// (The method *must* start with "set" to ensure other methods don't get called.)
				if (updateObject != null) {

					// Loop over all the properties, setting each one.
					for (Entry<String, Object> property : mUpdateProperties.entrySet()) {
						// Execute the "set" method against the child object.
						// (The method *must* start with "get" to ensure other methods don't get called.)
						String propertyName = property.getKey();
						Object propertyValue = property.getValue();
						String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
						//String setterName = "set" + propertyName;
						java.lang.reflect.Method method = classObject.getMethod(setterName, propertyValue.getClass());
						method.invoke(updateObject, propertyValue);
					}

					try {
						dao.store(updateObject);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}

					// Convert the list of objects into a JSon object.
					mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					JsonNode searchListNode = mapper.valueToTree(updateObject);
					dataNode.put(RESULTS, searchListNode);

					result = new WebSessionRespCmdObjectUpdate(dataNode);
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
		} catch (JsonParseException e) {
			LOGGER.error("", e);
		} catch (JsonMappingException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		return result;
	}
}
