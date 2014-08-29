/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ObjectFilterWsReqCmd.java,v 1.2 2013/04/07 07:14:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;
import com.gadgetworks.codeshelf.ws.IWebSession;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.ObjectFilterWsRespCmd;

/**
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_FILTER_REQ,
 * 	data {
 *		className:		<class_name>,
 *		propertyNames:[ <property>, <property>, <property> ],
 *		whereClause:	<where_clause>
 * 	}
 * }
 *
 * @author jeffw
 *
 */
public class ObjectFilterWsReqCmd extends WsReqCmdABC implements IWsPersistentReqCmd {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(ObjectListenerWsReqCmd.class);

	private Class<IDomainObject>			mPersistenceClass;
	private List<IDomainObject>				mObjectMatchList;
	private List<String>					mPropertyNames;
	private String							mFilterClause;
	private Map<String, Object>				mFilterParams;
	private IDaoProvider					mDaoProvider;
	private List<ITypedDao<IDomainObject>>	mDaoList;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public ObjectFilterWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mFilterParams = new HashMap<String, Object>();
		mDaoList = new ArrayList<ITypedDao<IDomainObject>>();
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.OBJECT_FILTER_REQ;
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
			String objectClassName = objectClassNode.asText();
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.domain." + objectClassName;
			}
			JsonNode propertyNamesNode = dataJsonNode.get(PROPERTY_NAME_LIST);
			ObjectMapper mapper = new ObjectMapper();
			mPropertyNames = null; // mapper.readValue(propertyNamesNode, new TypeReference<List<String>>() {});
			JsonNode filterClauseNode = dataJsonNode.get(FILTER_CLAUSE);
			mFilterClause = filterClauseNode.asText();

			JsonNode propertiesNode = dataJsonNode.get(FILTER_PARAMS);
			List<Map<String, Object>> objectArray = null; // mapper.readValue(propertiesNode, new TypeReference<List<Map<String, Object>>>() {});
			for (Map<String, Object> map : objectArray) {
				String name = (String) map.get("name");
				Object value = map.get("value");
				mFilterParams.put(name, value);
			}

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				mPersistenceClass = (Class<IDomainObject>) classObject;

				ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) mPersistenceClass);
				mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
				mDaoList.add(dao);

				result = getProperties(mObjectMatchList, OP_TYPE_UPDATE);
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SecurityException e) {
			LOGGER.error("", e);
		} catch (Exception e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private IWsRespCmd getProperties(List<IDomainObject> inDomainObjectList, String inOperationType) {

		IWsRespCmd result = null;

		try {

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : inDomainObjectList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();
				// Always include the class name and persistent ID in the results.
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
					// Capitalize the property name to invoke the getter for it.
					if (propertyName.isEmpty())
						LOGGER.error("empty property name in IWsRespCmd. Probable bug in js domainObject");  // Used to throw on charAt(0), which made this totally fail out.
						// How do you get here? Indicates a bug. Seen when slot had and error in domainObjects.js.
					else {
						String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
						String rememberGetterName = getterName;

						try {
							java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
							Class<?> methodReturnType = method.getReturnType();
							Object resultObject = method.invoke(matchedObject, (Object[]) null);
							propertiesMap.put(propertyName, resultObject);
						} catch (NoSuchMethodException e) {
							// Minor problem. UI hierarchical view asks for same data field name for all object types in the view. Not really an error in most cases
							LOGGER.debug("Method not found in ObjectFilterWsReqCmd getProperties: " + rememberGetterName);
						}
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

				result = new ObjectFilterWsRespCmd(dataNode);
			}

		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	public final IWsRespCmd processObjectAdd(IDomainObject inDomainObject) {
		ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) mPersistenceClass);
		mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_CREATE);
	}

	public final IWsRespCmd processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		// FIrst see if any changed properties match the filtered properties.
		boolean matchedChangedProperty = false;
		for (String propertyName : mPropertyNames) {
			if ((inChangedProperties != null) && (inChangedProperties.contains(propertyName))) {
				matchedChangedProperty = true;
				break;
			}
		}

		if (matchedChangedProperty) {
			ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) mPersistenceClass);
			mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
			List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
			domainObjectList.add(inDomainObject);
			if (mObjectMatchList.contains(inDomainObject)) {
				return getProperties(domainObjectList, OP_TYPE_UPDATE);
			} else {
				return getProperties(domainObjectList, OP_TYPE_DELETE);
			}
		} else {
			return null;
		}
	}

	public final IWsRespCmd processObjectDelete(IDomainObject inDomainObject) {
		// Delete is a bit of a weird case.  We don't want to refresh the member list since we want the delete to propagate to anyone listening with a filter.
		//		IGenericDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) mPersistenceClass);
		//		mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
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
