/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ObjectGetterWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.ObjectGetterWsRespCmd;

/**
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_GETTER_REQ,
 * 	data {
 *		className:   	<class_name>,
 *		persistentId:	<persistentId>,
 *		getterMethod:	<getterMethod>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class ObjectGetterWsReqCmd extends WsReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ObjectGetterWsReqCmd.class);

	private IDaoProvider		mDaoProvider;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public ObjectGetterWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.OBJECT_GETTER_REQ;
	}

	public final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {

			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode parentClassNode = dataJsonNode.get(CLASSNAME);
			String parentClassName = parentClassNode.getTextValue();
			if (!parentClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				parentClassName = "com.gadgetworks.codeshelf.model.domain." + parentClassName;
			}
			JsonNode parentIdNode = dataJsonNode.get(PERSISTENT_ID);
			UUID parentId = UUID.fromString(parentIdNode.getTextValue());
			JsonNode getMethodNode = dataJsonNode.get(GETTER_METHOD);
			String getterMethodName = getMethodNode.getTextValue();

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(parentClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {

				ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				IDomainObject parentObject = dao.findByPersistentId(parentId);

				// Execute the "get" method against the parent to return the children.
				// (The method *must* start with "get" to ensure other methods don't get called.)
				if (getterMethodName.startsWith("get")) {
					Object resultObject = null;
					try {
						java.lang.reflect.Method method = parentObject.getClass().getMethod(getterMethodName, (Class<?>[]) null);
						resultObject = method.invoke(parentObject, (Object[]) null);
					} catch (NoSuchMethodException e) {
						LOGGER.error("Method not found", e);
					}

					// Convert the list of objects into a JSon object.
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					ArrayNode searchListNode = mapper.valueToTree(resultObject);
					dataNode.put(RESULTS, searchListNode);

					result = new ObjectGetterWsRespCmd(dataNode);
				}
			}

		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SecurityException e) {
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
