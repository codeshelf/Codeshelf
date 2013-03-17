/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ObjectListenerWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;
import com.gadgetworks.codeshelf.ws.IWebSession;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.ObjectListenerWsRespCmd;

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
public class ObjectListenerWsReqCmd extends WsReqCmdABC implements IWsPersistentReqCmd {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(ObjectFilterWsReqCmd.class);

	private Class<IDomainObject>			mPersistenceClass;
	private List<IDomainObject>				mObjectMatchList;
	private List<UUID>						mObjectIdList;
	private List<String>					mPropertyNames;
	private IDaoProvider					mDaoProvider;
	private List<ITypedDao<IDomainObject>>	mDaoList;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public ObjectListenerWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mDaoList = new ArrayList<ITypedDao<IDomainObject>>();
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.OBJECT_LISTENER_REQ;
	}

	public final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		// CRITICAL SECURITYY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {
			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode objectClassNode = dataJsonNode.get(CLASSNAME);
			String objectClassName = objectClassNode.getTextValue();
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.domain." + objectClassName;
			}
			JsonNode objectIdListNode = dataJsonNode.get(OBJECT_ID_LIST);
			ObjectMapper mapper = new ObjectMapper();
			mObjectIdList = mapper.readValue(objectIdListNode, new TypeReference<List<UUID>>() {
			});
			JsonNode propertyNamesNode = dataJsonNode.get(PROPERTY_NAME_LIST);
			mPropertyNames = mapper.readValue(propertyNamesNode, new TypeReference<List<String>>() {
			});

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				mPersistenceClass = (Class<IDomainObject>) classObject;

				ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) mPersistenceClass);
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
	private final IWsRespCmd getProperties(List<IDomainObject> inDomainObjectList, String inOperationType) {

		IWsRespCmd result = null;

		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : inDomainObjectList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();
				// Always include the class naem and persistent ID in the results.
				propertiesMap.put(CLASSNAME, matchedObject.getClassName());
				propertiesMap.put(OP_TYPE, inOperationType);
				propertiesMap.put(PERSISTENT_ID, matchedObject.getPersistentId());
				// If this is a tree object then get the parent ID as well.
				if (matchedObject instanceof IDomainObjectTree<?>) {
					propertiesMap.put(PARENT_ID, ((IDomainObjectTree<?>) matchedObject).getParent().getPersistentId());
				}
				for (String propertyName : mPropertyNames) {
					// Execute the "get" method against the parents to return the children.
					// (The method *must* start with "get" to ensure other methods don't get called.)
					try {
						String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
						//String getterName = "get" + propertyName;
						java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
						Object resultObject = method.invoke(matchedObject, (Object[]) null);
						propertiesMap.put(propertyName, resultObject);
					} catch (NoSuchMethodException e) {
						LOGGER.error("Method not found", e);
					}
				}
				resultsList.add(propertiesMap);
			}

			if (resultsList.size() > 0) {
				// Convert the list of objects into a JSon object.
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode dataNode = mapper.createObjectNode();
				ArrayNode searchListNode = mapper.valueToTree(resultsList);
				dataNode.put(RESULTS, searchListNode);

				result = new ObjectListenerWsRespCmd(dataNode);
			}
		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	public final IWsRespCmd processObjectAdd(IDomainObject inDomainObject) {
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_CREATE);
	}

	public final IWsRespCmd processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_UPDATE);
	}

	public final IWsRespCmd processObjectDelete(IDomainObject inDomainObject) {
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_DELETE);
	}

	public final void registerSessionWithDaos(IWebSession inWebSession) {
		for (ITypedDao<IDomainObject> dao : mDaoList) {
			dao.registerDAOListener(inWebSession);
		}
	}

	public final void unregisterSessionWithDaos(IWebSession inWebSession) {
		for (ITypedDao<IDomainObject> dao : mDaoList) {
			dao.unregisterDAOListener(inWebSession);
		}
	}

	public final Class<IDomainObject> getPersistenceClass() {
		return mPersistenceClass;
	}
}
