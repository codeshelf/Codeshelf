/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ObjectUpdateWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.ObjectMethodWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.ObjectUpdateWsRespCmd;

/**
 * 
 * INBOUND COMMAND STRUCTURE:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_UPDATE_REQ,
 * 	data {
 *		className:			<class_name>,
 *		parentPeristentID:	<id>,
 *		propertiesToSet [
 *			propertyName:	<propertyValue>
 *		]
 * 	}
 * }
 * 
 * OUTBOUND COMMAND STRUCTURE:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_UPDATE_RESP,
 * 	data {
 * 		newPersistentID: <id>
 *  }
 * }
 *
 * @author jeffw
 *
 */
public class ObjectUpdateWsReqCmd extends WsReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ObjectUpdateWsReqCmd.class);

	private IDaoProvider		mDaoProvider;
	//	private Map<String, Object>	mUpdateProperties;
	private List<ArgsClass>		mPropertyArguments;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public ObjectUpdateWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		//		mUpdateProperties = new HashMap<String, Object>();
		mPropertyArguments = new ArrayList<ArgsClass>();
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.OBJECT_UPDATE_REQ;
	}

	public final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {

			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode classNode = dataJsonNode.get(CLASSNAME);
			String className = classNode.getTextValue();
			if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				className = "com.gadgetworks.codeshelf.model.domain." + className;
			}
			JsonNode idNode = dataJsonNode.get(PERSISTENT_ID);
			UUID objectId = UUID.fromString(idNode.getTextValue());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode propertiesNode = dataJsonNode.get(PROPERTIES);

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				// First locate an instance of the parent class.
				ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				IDomainObject updateObject = dao.findByPersistentId(objectId);

				// Execute the "set" method against the parents to return the children.
				// (The method *must* start with "set" to ensure other methods don't get called.)
				if (updateObject != null) {
					ObjectMapper objectSetter = new ObjectMapper();
					updateObject = objectSetter.readerForUpdating(updateObject).readValue(propertiesNode);
					
					dao.store(updateObject);
					mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					JsonNode updatedObjectNode = mapper.valueToTree(updateObject);
					dataNode.put(RESULTS, updatedObjectNode);
					result = new ObjectUpdateWsRespCmd(dataNode);
				}
				else {
					ObjectNode errorNode = createErrorResult("Object of type " + classObject + " not found with id: " + objectId );
					result = new ObjectMethodWsRespCmd(errorNode);
				}
			}
		} catch (DaoException e) {
			ObjectNode errorNode = createErrorResult(e.toString());
			result = new ObjectMethodWsRespCmd(errorNode);
		} catch (IllegalArgumentException e) {
			ObjectNode errorNode = createErrorResult(e.toString());
			result = new ObjectMethodWsRespCmd(errorNode);
		} catch (JsonParseException e) {
			ObjectNode errorNode = createErrorResult(e.toString());
			result = new ObjectMethodWsRespCmd(errorNode);
		} catch (JsonMappingException e) {
			ObjectNode errorNode = createErrorResult(e.toString());
			result = new ObjectMethodWsRespCmd(errorNode);
		} catch (IOException e) {
			ObjectNode errorNode = createErrorResult(e.toString());
			result = new ObjectMethodWsRespCmd(errorNode);
		} catch (ClassNotFoundException e) {
			ObjectNode errorNode = createErrorResult(e.toString());
			result = new ObjectMethodWsRespCmd(errorNode);
		}
		return result;
	}

	private ObjectNode createErrorResult(String... messages) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode resultNode = mapper.createObjectNode();
		resultNode.put("messages", mapper.valueToTree(Arrays.asList(messages)));

		ObjectNode dataNode = mapper.createObjectNode();
		dataNode.put(STATUS_ELEMENT, "ERROR");
		dataNode.put(RESULTS, resultNode);
		return dataNode;
	}
}
