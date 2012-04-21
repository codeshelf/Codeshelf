/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectFilter.java,v 1.7 2012/04/21 08:23:29 jeffw Exp $
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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
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

	private Class<PersistABC>				mPersistenceClass;
	private List<PersistABC>				mObjectMatchList;
	private List<String>					mPropertyNames;
	private String							mFilterClause;
	private Map<String, Object>				mFilterParams;
	private IDaoProvider					mDaoProvider;
	private List<IGenericDao<PersistABC>>	mDaoList;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectFilter(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mFilterParams = new HashMap<String, Object>();
		mDaoList = new ArrayList<IGenericDao<PersistABC>>();
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
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.persist.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.persist." + objectClassName;
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
			if (PersistABC.class.isAssignableFrom(classObject)) {
				mPersistenceClass = (Class<PersistABC>) classObject;

				IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) mPersistenceClass);
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
	private IWebSessionRespCmd getProperties(List<PersistABC> inDomainObjectList, String inOperationType) {

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

			result = new WebSessionRespCmdObjectFilter(dataNode);

		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	public final IWebSessionRespCmd processObjectAdd(PersistABC inDomainObject) {
		IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) mPersistenceClass);
		mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_CREATE);
	}

	public final IWebSessionRespCmd processObjectUpdate(PersistABC inDomainObject) {
		IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) mPersistenceClass);
		mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
		return getProperties(domainObjectList, OP_TYPE_UPDATE);
	}

	public final IWebSessionRespCmd processObjectDelete(PersistABC inDomainObject) {
		IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) mPersistenceClass);
		mObjectMatchList = dao.findByFilter(mFilterClause, mFilterParams);
		List<PersistABC> domainObjectList = new ArrayList<PersistABC>();
		if (mObjectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
		}
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
