/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectListener.java,v 1.12 2012/07/11 07:15:42 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
public class WebSessionReqCmdObjectListener extends WebSessionReqCmdABC implements IWebSessionPersistentReqCmd {

	private static final Log				LOGGER	= LogFactory.getLog(WebSessionReqCmdObjectFilter.class);

	private Class<PersistABC>				mPersistenceClass;
	private List<PersistABC>				mObjectMatchList;
	private List<Long>						mObjectIdList;
	private List<String>					mPropertyNames;
	private IDaoProvider					mDaoProvider;
	private List<ITypedDao<PersistABC>>	mDaoList;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectListener(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mDaoList = new ArrayList<ITypedDao<PersistABC>>();
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
			JsonNode objectClassNode = dataJsonNode.get(CLASSNAME);
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

				ITypedDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) mPersistenceClass);
				mObjectMatchList = dao.findByPersistentIdList(mObjectIdList);
				mDaoList.add(dao);

				result = getProperties(mObjectMatchList, OP_TYPE_UPDATE);
			}
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
	private final IWebSessionRespCmd getProperties(List<PersistABC> inDomainObjectList, String inOperationType) {

		IWebSessionRespCmd result = null;

		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (PersistABC matchedObject : inDomainObjectList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();
				// Always include the class naem and persistent ID in the results.
				propertiesMap.put(CLASSNAME, matchedObject.getClassName());
				propertiesMap.put(OP_TYPE, inOperationType);
				propertiesMap.put(PERSISTENT_ID, matchedObject.getPersistentId());
				for (String propertyName : mPropertyNames) {
					// Execute the "get" method against the parents to return the children.
					// (The method *must* start with "get" to ensure other methods don't get called.)
					try {
						String getterName = "get" + propertyName;
						java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
						Object resultObject = method.invoke(matchedObject, (Object[]) null);
						propertiesMap.put(propertyName, resultObject);
					} catch (NoSuchMethodException e) {
						LOGGER.error("Method not found", e);
					}
				}
				resultsList.add(propertiesMap);
			}

			// Convert the list of objects into a JSon object.
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode dataNode = mapper.createObjectNode();
			ArrayNode searchListNode = mapper.valueToTree(resultsList);
			dataNode.put(RESULTS, searchListNode);

			result = new WebSessionRespCmdObjectListener(dataNode);

		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	public final IWebSessionRespCmd processObjectAdd(PersistABC inDomainObject) {
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_CREATE);
	}

	public final IWebSessionRespCmd processObjectUpdate(PersistABC inDomainObject) {
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_UPDATE);
	}

	public final IWebSessionRespCmd processObjectDelete(PersistABC inDomainObject) {
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_DELETE);
	}

	public final void registerSessionWithDaos(IWebSession inWebSession) {
		for (ITypedDao<PersistABC> dao : mDaoList) {
			dao.registerDAOListener(inWebSession);
		}
	}

	public final void unregisterSessionWithDaos(IWebSession inWebSession) {
		for (ITypedDao<PersistABC> dao : mDaoList) {
			dao.unregisterDAOListener(inWebSession);
		}
	}

}
