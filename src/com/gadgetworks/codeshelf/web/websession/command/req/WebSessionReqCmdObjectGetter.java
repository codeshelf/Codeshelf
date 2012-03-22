/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectGetter.java,v 1.4 2012/03/22 07:35:11 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectGetter;

/**
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectGetter extends WebSessionReqCmdABC {

	private static final Log		LOGGER				= LogFactory.getLog(WebSessionReqCmdObjectGetter.class);

	private static final String		CLASS_NODE			= "className";
	private static final String		ID_NODE				= "persistentId";
	private static final String		GETTER_METHOD		= "getterMethod";
	private static final String		OBJECT_RESULTS_NODE	= "result";

	private IDbFacade<PersistABC>	mDbFacade;
	private IDaoProvider			mDaoProvider;

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
			JsonNode parentClassNode = dataJsonNode.get(CLASS_NODE);
			String parentClass = parentClassNode.getTextValue();
			JsonNode parentIdNode = dataJsonNode.get(ID_NODE);
			long parentId = parentIdNode.getLongValue();
			JsonNode getMethodNode = dataJsonNode.get(GETTER_METHOD);
			String getterMethodName = getMethodNode.getTextValue();

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(parentClass);
			if (PersistABC.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				//				Class<?> clazz = classObject.getClass();
				//@SuppressWarnings("unchecked")
				//PersistABC parentObject = mDbFacade.findByPersistentId(classObject, parentId);
				IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) classObject);
				PersistABC parentObject = dao.loadByPersistentId(parentId);

				// Execute the "get" method against the parents to return the children.
				// (The method *must* start with "get" to ensure other methods don't get called.)
				if (getterMethodName.startsWith("get")) {
					java.lang.reflect.Method method = parentObject.getClass().getMethod(getterMethodName, (Class<?>[]) null);
					Object resultObject = method.invoke(parentObject, (Object[]) null);

					// Convert the list of ojects into a JSon object.
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					ArrayNode searchListNode = mapper.valueToTree(resultObject);
					dataNode.put(OBJECT_RESULTS_NODE, searchListNode);

					result = new WebSessionRespCmdObjectGetter(dataNode);
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

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd#doesPersist()
	 */
	public final boolean doesPersist() {
		return false;
	}
}
