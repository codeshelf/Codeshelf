/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectFilter.java,v 1.2 2012/03/24 18:28:01 jeffw Exp $
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
public class WebSessionReqCmdObjectFilter extends WebSessionReqCmdABC {
	private static final Log				LOGGER				= LogFactory.getLog(WebSessionReqCmdObjectListener.class);

	private static final String				OBJECT_CLASS		= "className";
	private static final String				PROPERTY_NAME_LIST	= "propertyNames";
	private static final String				FILTER_CLAUSE		= "filterClause";
	private static final String				OBJECT_RESULTS_NODE	= "result";

	private Class<PersistABC>				mPersistenceClass;
	private List<String>					mPropertyNames;
	private String							mFilterClause;
	private IDaoProvider					mDaoProvider;
	private List<IGenericDao<PersistABC>>	mDaoList;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectFilter(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
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
			JsonNode objectClassNode = dataJsonNode.get(OBJECT_CLASS);
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

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (PersistABC.class.isAssignableFrom(classObject)) {
				mPersistenceClass = (Class<PersistABC>) classObject;
				result = getProperties();
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
	private final IWebSessionRespCmd getProperties() {

		IWebSessionRespCmd result = null;

		try {
			IGenericDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) mPersistenceClass);
			List<PersistABC> objectList = dao.findByFilter(mFilterClause);
			mDaoList.add(dao);

			List<Map<String, String>> resultsList = new ArrayList<Map<String, String>>();
			for (PersistABC matchedObject : objectList) {
				Map<String, String> propertiesMap = new HashMap<String, String>();
				// Always include the class naem and persistent ID in the results.
				propertiesMap.put("ClassName", matchedObject.getClassName().toString());
				propertiesMap.put("persistentId", matchedObject.getPersistentId().toString());
				for (String propertyName : mPropertyNames) {
					// Execute the "get" method against the parents to return the children.
					// (The method *must* start with "get" to ensure other methods don't get called.)
					String getterName = "get" + propertyName;
					java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
					Object resultObject = method.invoke(matchedObject, (Object[]) null);
					propertiesMap.put(propertyName, resultObject.toString());
				}
				resultsList.add(propertiesMap);
			}

			// Convert the list of objects into a JSon object.
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode dataNode = mapper.createObjectNode();
			ArrayNode searchListNode = mapper.valueToTree(resultsList);
			dataNode.put(OBJECT_RESULTS_NODE, searchListNode);

			result = new WebSessionRespCmdObjectFilter(dataNode);

		} catch (NoSuchMethodException e) {
			LOGGER.error("", e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
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

	public final IWebSessionRespCmd getResponseCmd() {
		return getProperties();
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
