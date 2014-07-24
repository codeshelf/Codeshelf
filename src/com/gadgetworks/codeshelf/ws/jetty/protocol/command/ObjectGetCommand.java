package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectGetRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectGetResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class ObjectGetCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectGetCommand.class);

	private ObjectGetRequest request;
	
	public ObjectGetCommand(ObjectGetRequest request) {
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		ObjectGetResponse response = new ObjectGetResponse();
		
		try {
			String parentClassName = request.getClassName();
			if (!parentClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				parentClassName = "com.gadgetworks.codeshelf.model.domain." + parentClassName;
			}
			UUID parentId = UUID.fromString(request.getPersistentId());
			String getterMethodName = request.getGetterMethod();

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(parentClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				
				ITypedDao<IDomainObject> dao = daoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				
				IDomainObject parentObject = dao.findByPersistentId(parentId);
				// Execute the "get" method against the parent to return the children.
				// (The method *must* start with "get" to ensure other methods don't get called.)
				if (getterMethodName.startsWith("get")) {
					Object results = null;
					try {
						java.lang.reflect.Method method = parentObject.getClass().getMethod(getterMethodName, (Class<?>[]) null);
						results = method.invoke(parentObject, (Object[]) null);
						response.setResults(results);
						response.setStatus(ResponseStatus.Success);
						return response;
					} catch (NoSuchMethodException e) {
						LOGGER.error("Method not found", e);
					}
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage("Method not found");
					return response;
					// Convert the list of objects into a JSon object.
					/*
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					ArrayNode searchListNode = mapper.valueToTree(resultObject);
					dataNode.put(RESULTS, searchListNode);
					*/
					// result = new ObjectGetterWsRespCmd(dataNode);
				}
			}

		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SecurityException e) {
			LOGGER.error("", e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
