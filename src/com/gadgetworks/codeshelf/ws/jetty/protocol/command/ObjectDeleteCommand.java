package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectDeleteRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectDeleteResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class ObjectDeleteCommand extends CommandABC {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectDeleteCommand.class);

	private ObjectDeleteRequest	request;
	
	public ObjectDeleteCommand(IDaoProvider daoProvider, UserSession session, ObjectDeleteRequest request) {
		super(daoProvider, session);
		this.request = request;
	}
	
	@Override
	public ResponseABC exec() {
		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {
			String className = request.getClassName();
			if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				className = "com.gadgetworks.codeshelf.model.domain." + className;
			}
			UUID objectIdId = UUID.fromString(request.getPersistentId());

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				@SuppressWarnings("unchecked")
				ITypedDao<IDomainObject> dao = this.daoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				IDomainObject object = dao.findByPersistentId(objectIdId);

				// Execute the "set" method against the parents to return the children.
				// (The method *must* start with "set" to ensure other methods don't get called.)
				if (object != null) {
					dao.delete(object);
					ObjectDeleteResponse response = new ObjectDeleteResponse();
					response.setResults(object);
					response.setStatus(ResponseStatus.Success);
					return response;
				}
				else {
					ObjectDeleteResponse response = new ObjectDeleteResponse();
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage("Object not found");
					return response;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to delete object", e);
		}
		ObjectDeleteResponse response = new ObjectDeleteResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
