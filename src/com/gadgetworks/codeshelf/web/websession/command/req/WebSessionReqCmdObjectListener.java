/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectListener.java,v 1.7 2012/03/30 23:21:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

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

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectListener;

/**
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_LISTENER_REQ,
 * 	data {
 *		className:    <class_name>,
 *		objectIds:    [ <persistentId>, <persistentId>, <persistentId> ],
 *		propertyNames:[ <property>, <property>, <property> ]
 * 	}
 * }
 *
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectListener extends WebSessionReqCmdABC {

	private static final Log				LOGGER				= LogFactory.getLog(WebSessionReqCmdObjectFilter.class);

	private static final String				OBJECT_CLASS		= "className";
	private static final String				OBJECT_ID_LIST		= "objectIds";
	private static final String				PROPERTY_NAME_LIST	= "propertyNames";
	private static final String				PERSISTENT_ID		= "persistentId";
	private static final String				OP_TYPE				= "opType";
	private static final String				OBJECT_RESULTS_NODE	= "result";

	private static final String				OP_TYPE_ADD			= "add";
	private static final String				OP_TYPE_UPDATE		= "update";
	private static final String				OP_TYPE_DELETE		= "delete";

	private Class<PersistABC>				mPersistenceClass;
	private List<PersistABC>				mObjectMatchList;
	private List<Long>						mObjectIdList;
	private List<String>					mPropertyNames;
	private IDaoProvider					mDaoProvider;
	private List<IGenericDao<PersistABC>>	mDaoList;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectListener(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mDaoList = new ArrayList<IGenericDao<PersistABC>>();
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
			String objectClassName = objectClassNode.getTextValue();
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.persist.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.persist." + objectClassName;
			}
			JsonNode objectIdListNode = dataJsonNode.get(OBJECT_ID_LIST);
			ObjectMapper mapper = new ObjectMapper();
			mObjectIdList = mapper.readValue(objectIdListNode, new TypeReference<List<Long>>() {
			});
			JsonNode propertyNamesNode = dataJsonNode.get(PROPERTY_NAME_LIST);
			mPropertyNames = mapper.readValue(propertyNamesNode, new TypeReference<List<String>>() {
			});

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (PersistABC.class.isAssignableFrom(classObject)) {
				mPersistenceClass = (Class<PersistABC>) classObject;
				result = getProperties(mObjectMatchList, OP_TYPE_ADD);
			}
			
			IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) mPersistenceClass);
			mObjectMatchList = dao.findByPersistentIdList(mObjectIdList);
			mDaoList.add(dao);
		} catch (IOException e) {
			LOGGER.error("", e);
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SecurityException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final IWebSessionRespCmd getProperties(List<PersistABC> inDomainObjectList, String inOperationType) {

		IWebSessionRespCmd result = null;

		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (PersistABC matchedObject : inDomainObjectList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();
				// Always include the class naem and persistent ID in the results.
				propertiesMap.put(OBJECT_CLASS, matchedObject.getClassName().toString());
				propertiesMap.put(OP_TYPE, inOperationType);
				propertiesMap.put(PERSISTENT_ID, matchedObject.getPersistentId().toString());
				for (String propertyName : mPropertyNames) {
					// Execute the "get" method against the parents to return the children.
					// (The method *must* start with "get" to ensure other methods don't get called.)
					String getterName = "get" + propertyName;
					java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
					Object resultObject = method.invoke(matchedObject, (Object[]) null);
					propertiesMap.put(propertyName, resultObject);
				}
				resultsList.add(propertiesMap);
			}

			// Convert the list of objects into a JSon object.
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode dataNode = mapper.createObjectNode();
			ArrayNode searchListNode = mapper.valueToTree(resultsList);
			dataNode.put(OBJECT_RESULTS_NODE, searchListNode);

			result = new WebSessionRespCmdObjectListener(dataNode);

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
		return true;
	}

	public final IWebSessionRespCmd processObjectAdd(PersistABC inDomainObject) {
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		domainObjectList.add(inDomainObject);
		return getProperties(domainObjectList, OP_TYPE_ADD);
	}

	public final IWebSessionRespCmd processObjectUpdate(PersistABC inDomainObject) {
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		domainObjectList.add(inDomainObject);
		return getProperties(domainObjectList, OP_TYPE_UPDATE);
	}

	public final IWebSessionRespCmd processObjectDelete(PersistABC inDomainObject) {
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		domainObjectList.add(inDomainObject);
		return getProperties(domainObjectList, OP_TYPE_DELETE);
	}

	public final void registerSessionWithDaos(IWebSession inWebSession) {
		for (IGenericDao<PersistABC> dao : mDaoList) {
			dao.registerDAOListener(inWebSession);
		}
	}

	public final void unregisterSessionWithDaos(IWebSession inWebSession) {
		for (IGenericDao<PersistABC> dao : mDaoList) {
			dao.unregisterDAOListener(inWebSession);
		}
	}

}
