/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectUpdate.java,v 1.8 2012/04/10 08:01:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectUpdate;

/**
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_LISTENER_REQ,
 * 	data {
 *		className:		<class_name>,
 *		persistentId:	<persistentId>,
 *		setterMethod:	<setterMethod>,
 *		setterValue: 	<setterValue>
 * 	}
 * }
 *
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectUpdate extends WebSessionReqCmdABC {

	private static final Log	LOGGER				= LogFactory.getLog(WebSessionReqCmdObjectUpdate.class);

	private IDaoProvider		mDaoProvider;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectUpdate(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
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
			JsonNode parentClassNode = dataJsonNode.get(CLASSNAME);
			String parentClassName = parentClassNode.getTextValue();
			if (!parentClassName.startsWith("com.gadgetworks.codeshelf.model.persist.")) {
				parentClassName = "com.gadgetworks.codeshelf.model.persist." + parentClassName;
			}
			JsonNode parentIdNode = dataJsonNode.get(PERSISTENT_ID);
			long parentId = parentIdNode.getLongValue();
			JsonNode setterMethodNode = dataJsonNode.get(SETTER_METHOD);
			String setterMethodName = setterMethodNode.getTextValue();
			JsonNode setterValueNode = dataJsonNode.get(SETTER_VALUE);
			String setterValue = setterValueNode.getTextValue();

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(parentClassName);
			if (PersistABC.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) classObject);
				PersistABC parentObject = dao.findByPersistentId(parentId);

				// Execute the "set" method against the parents to return the children.
				// (The method *must* start with "set" to ensure other methods don't get called.)
				if ((parentObject != null) && (setterMethodName.startsWith("set"))) {
					Class<?>[] types = new Class<?>[] { String.class };
					Object[] value = new Object[] { setterValue };
					java.lang.reflect.Method method = parentObject.getClass().getMethod(setterMethodName, types);
					Object resultObject = method.invoke(parentObject, value);
					try {
						dao.store(parentObject);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}

					// Convert the list of objects into a JSon object.
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					ArrayNode searchListNode = mapper.valueToTree(resultObject);
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
		}

		return result;
	}
}
