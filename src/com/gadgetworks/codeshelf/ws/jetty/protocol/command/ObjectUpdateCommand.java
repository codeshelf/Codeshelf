package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectUpdateResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class ObjectUpdateCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectUpdateCommand.class);

	private ObjectUpdateRequest	request;
	
	private PropertyUtilsBean propertyUtil = new PropertyUtilsBean();
	
	public ObjectUpdateCommand(IDaoProvider daoProvider, CsSession session, ObjectUpdateRequest request) {
		super(daoProvider, session);
		this.request = request;
	}
	
	@Override
	public ResponseABC exec() {
		ObjectUpdateResponse response = new ObjectUpdateResponse();
		
		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		// extract UUID
		String className = request.getClassName();
		String persistentId = request.getPersistentId();
		if (persistentId==null) {
			LOGGER.error("Failed to update "+className+":  Object ID is undefined");
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Object ID is not defined");
			return response;
		}
		UUID objectId = null;
		try {
			objectId = UUID.fromString(persistentId);
		}
		catch (Exception e) {
			LOGGER.error("Failed to update "+className,e);
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Failed to convert object ID "+persistentId+" to UUID");
			return response;
		}

		if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
			className = "com.gadgetworks.codeshelf.model.domain." + className;
		}
		
		Map<String, Object> properties = request.getProperties();
		try {
			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				// First locate an instance of the parent class.
				ITypedDao<IDomainObject> dao = daoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				IDomainObject updateObject = dao.findByPersistentId(objectId);

				// Execute the "set" method against the parents to return the children.
				// (The method *must* start with "set" to ensure other methods don't get called.)
				if (updateObject != null) {
					// update object...
					// ORIGNIAL CODE:
					// ObjectMapper objectSetter = new ObjectMapper();
					// updateObject = objectSetter.readerForUpdating(updateObject).readValue(properties);
					Map<String, Throwable> failures = new HashMap<String, Throwable>();
					for (Map.Entry<String, Object> property : properties.entrySet()) {
						try {
							propertyUtil.setProperty(updateObject, property.getKey(), property.getValue());
						}
						catch(InvocationTargetException e) {
							failures.put(property.getKey(), e.getTargetException());
						}
						catch(Exception e) {
							failures.put(property.getKey(), e);
						}
					}
					if (failures.isEmpty()) {
						dao.store(updateObject);	
						// create response
						response.setResults(updateObject);
						response.setStatus(ResponseStatus.Success);
						return response;
					}
					else {
						response.setStatus(ResponseStatus.Fail);
						response.setStatusMessage("Some properties failed to update: " + failures.toString());
						return response;
					}
				}
				else {
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage(className+" with ID #"+objectId+" not found");
					return response;
				}
			}
		}
		catch (Exception e) {
			LOGGER.error("Failed to update object "+className+"("+objectId+")",e);
		}
		response.setStatus(ResponseStatus.Fail);
		response.setStatusMessage("Update failed");
		return response;
	}

}