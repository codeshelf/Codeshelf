/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectGetter.java,v 1.10 2012/04/21 08:23:29 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectGetter;

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
public class WebSessionReqCmdObjectGetter extends WebSessionReqCmdABC {

	private static final Log	LOGGER	= LogFactory.getLog(WebSessionReqCmdObjectGetter.class);

	private IDaoProvider		mDaoProvider;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectGetter(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.OBJECT_GETTER_REQ;
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
			JsonNode parentClassNode = dataJsonNode.get(CLASSNAME);
			String parentClassName = parentClassNode.getTextValue();
			if (!parentClassName.startsWith("com.gadgetworks.codeshelf.model.persist.")) {
				parentClassName = "com.gadgetworks.codeshelf.model.persist." + parentClassName;
			}
			JsonNode parentIdNode = dataJsonNode.get(PERSISTENT_ID);
			long parentId = parentIdNode.getLongValue();
			JsonNode getMethodNode = dataJsonNode.get(GETTER_METHOD);
			String getterMethodName = getMethodNode.getTextValue();

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(parentClassName);
			if (PersistABC.class.isAssignableFrom(classObject)) {

				IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) classObject);
				PersistABC parentObject = dao.findByPersistentId(parentId);

				// Execute the "get" method against the parents to return the children.
				// (The method *must* start with "get" to ensure other methods don't get called.)
				if (getterMethodName.startsWith("get")) {
					Object resultObject = null;
					try {
						java.lang.reflect.Method method = parentObject.getClass().getMethod(getterMethodName, (Class<?>[]) null);
						resultObject = method.invoke(parentObject, (Object[]) null);
					} catch (NoSuchMethodException e) {
						LOGGER.error("Method not found", e);
					}

					// Convert the list of ojects into a JSon object.
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					ArrayNode searchListNode = mapper.valueToTree(resultObject);
					dataNode.put(RESULTS, searchListNode);

					result = new WebSessionRespCmdObjectGetter(dataNode);
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
