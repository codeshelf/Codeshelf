/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectFilter.java,v 1.11 2012/07/29 09:30:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectFilter;

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
public class WebSessionReqCmdObjectFilter extends WebSessionReqCmdABC implements IWebSessionPersistentReqCmd {
	private static final Log				LOGGER	= LogFactory.getLog(WebSessionReqCmdObjectListener.class);

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
	public WebSessionReqCmdObjectFilter(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mFilterParams = new HashMap<String, Object>();
		mDaoList = new ArrayList<ITypedDao<IDomainObject>>();
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.OBJECT_FILTER_REQ;
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
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.domain." + objectClassName;
			}
			JsonNode propertyNamesNode = dataJsonNode.get(PROPERTY_NAME_LIST);
			ObjectMapper mapper = new ObjectMapper();
			mPropertyNames = mapper.readValue(propertyNamesNode, new TypeReference<List<String>>() {
			});
			JsonNode filterClauseNode = dataJsonNode.get(FILTER_CLAUSE);
			mFilterClause = filterClauseNode.getTextValue();

			JsonNode propertiesNode = dataJsonNode.get(FILTER_PARAMS);
			List<Map<String, Object>> objectArray = mapper.readValue(propertiesNode, new TypeReference<List<Map<String, Object>>>() {
			});
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
		} catch (JsonParseException e) {
			LOGGER.error("", e);
		} catch (JsonMappingException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private IWebSessionRespCmd getProperties(List<IDomainObject> inDomainObjectList, String inOperationType) {

		IWebSessionRespCmd result = null;

		try {

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : inDomainObjectList) {
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

			if (resultsList.size() > 0) {
				// Convert the list of objects into a JSon object.
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode dataNode = mapper.createObjectNode();
				ArrayNode searchListNode = mapper.valueToTree(resultsList);
				dataNode.put(RESULTS, searchListNode);

				result = new WebSessionRespCmdObjectFilter(dataNode);
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

	public final IWebSessionRespCmd processObjectAdd(IDomainObject inDomainObject) {
		ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) mPersistenceClass);
		mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_CREATE);
	}

	public final IWebSessionRespCmd processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		// FIrst see if any changed properties match the filtered properties.
		boolean matchedChangedProperty = false;
		for (String propertyName : mPropertyNames) {
			if ((inChangedProperties != null) && (inChangedProperties.contains(propertyName))) {
				matchedChangedProperty = true;
			}
		}

		if (matchedChangedProperty) {
			ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) mPersistenceClass);
			mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
			List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
			if (mObjectMatchList.contains(inDomainObject)) {
				domainObjectList.add(inDomainObject);
			}
			return getProperties(domainObjectList, OP_TYPE_UPDATE);
		} else {
			return null;
		}
	}

	public final IWebSessionRespCmd processObjectDelete(IDomainObject inDomainObject) {
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
}
