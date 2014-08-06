package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public ObjectUpdateCommand(CsSession session, ObjectUpdateRequest request) {
		super(session);
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

		String className = request.getClassName();
		if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
			className = "com.gadgetworks.codeshelf.model.domain." + className;
		}
		UUID objectId = UUID.fromString(request.getPersistentId());
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
					BeanUtils.populate(updateObject, properties);
					
					dao.store(updateObject);
					
					// create response
					response.setResults(updateObject);
					response.setStatus(ResponseStatus.Success);
					return response;
				}
				else {
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage(classObject+" with ID #"+objectId+" not founds");
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
