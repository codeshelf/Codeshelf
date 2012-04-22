/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectCreate.java,v 1.3 2012/04/22 08:10:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectCreate;

/**
 * 
 * INBOUND COMMAND STRUCTURE:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_CREATE_REQ,
 * 	data {
 *		parentClassName:	<class_name>,
 *		parentPeristentID:	<id>,
 *		className:			<class_name>,
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
 * 	type: OBJECT_CREATE_RESP,
 * 	data {
 * 		newPersistentID: <id>
 *  }
 * }
 *
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectCreate extends WebSessionReqCmdABC {

	private static final Log	LOGGER	= LogFactory.getLog(WebSessionReqCmdObjectCreate.class);

	private IDaoProvider		mDaoProvider;
	private Map<String, Object>	mUpdateProperties;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectCreate(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
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
		// The remote end can NEVER set object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {

			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode parentClassNode = dataJsonNode.get(PARENT_CLASS);
			String parentClassName = parentClassNode.getTextValue();
			if (!parentClassName.startsWith("com.gadgetworks.codeshelf.model.persist.")) {
				parentClassName = "com.gadgetworks.codeshelf.model.persist." + parentClassName;
			}
			JsonNode parentIdNode = dataJsonNode.get(PARENT_ID);
			long parentId = parentIdNode.getLongValue();
			JsonNode childClassNode = dataJsonNode.get(CLASSNAME);
			String childClassName = childClassNode.getTextValue();
			if (!childClassName.startsWith("com.gadgetworks.codeshelf.model.persist.")) {
				childClassName = "com.gadgetworks.codeshelf.model.persist." + childClassName;
			}

			ObjectMapper mapper = new ObjectMapper();
			JsonNode propertiesNode = dataJsonNode.get(PROPERTIES);
			List<Map<String, Object>> objectArray = mapper.readValue(propertiesNode, new TypeReference<List<Map<String, Object>>>() {
			});
			for (Map<String, Object> map : objectArray) {
				String name = (String) map.get("name");
				Object value = map.get("value");
				mUpdateProperties.put(name, value);
			}

			mapper = new ObjectMapper();
			ObjectNode dataNode = mapper.createObjectNode();

			// First we find the class of the parent object.
			Class<?> parentClass = Class.forName(parentClassName);
			if ((parentClass != null) && (PersistABC.class.isAssignableFrom(parentClass))) {

				// First locate an instance of the parent class.
				IGenericDao<PersistABC> parentClassDao = mDaoProvider.getDaoInstance((Class<PersistABC>) parentClass);
				PersistABC parentObject = parentClassDao.findByPersistentId(parentId);

				// Then we find the class of the object we want to create.
				Class<?> childClass = Class.forName(childClassName);
				if ((childClass != null) && (PersistABC.class.isAssignableFrom(childClass))) {

					// First locate an instance of the parent class.
					IGenericDao<PersistABC> childClassDao = mDaoProvider.getDaoInstance((Class<PersistABC>) childClass);

					if (parentObject != null) {
						// Now create the new object as a child of the parent object.
						Constructor<?> ctor = childClass.getConstructor();
						Object object = ctor.newInstance();
						if (object instanceof PersistABC) {
							PersistABC newChildObject = (PersistABC) object;
							newChildObject.setParent(parentObject);

							// Loop over all the properties, setting each one.
							for (Entry<String, Object> property : mUpdateProperties.entrySet()) {
								// Execute the "set" method against the child object.
								// (The method *must* start with "get" to ensure other methods don't get called.)
								String propertyName = property.getKey();
								Object propertyValue = property.getValue();
								String setterName = "set" + propertyName;
								java.lang.reflect.Method method = childClass.getMethod(setterName, propertyValue.getClass());
								method.invoke(newChildObject, propertyValue);
							}
							childClassDao.store(newChildObject);

							// Convert the new object into a JSon object.
							mapper = new ObjectMapper();
							dataNode = mapper.createObjectNode();
							JsonNode searchListNode = mapper.valueToTree(newChildObject);
							dataNode.put(RESULTS, searchListNode);

							result = new WebSessionRespCmdObjectCreate(dataNode);
						}
					}
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
		} catch (DaoException e) {
			LOGGER.error("", e);
		} catch (JsonParseException e) {
			LOGGER.error("", e);
		} catch (JsonMappingException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		} catch (InstantiationException e) {
			LOGGER.error("", e);
		}

		return result;
	}
}
