package com.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.ws.jetty.protocol.request.ObjectDeleteRequest;
import com.codeshelf.ws.jetty.protocol.response.ObjectDeleteResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.UserSession;

public class ObjectDeleteCommand extends CommandABC {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectDeleteCommand.class);

	private ObjectDeleteRequest	request;
	
	public ObjectDeleteCommand(UserSession session, ObjectDeleteRequest request) {
		super(session);
		this.request = request;
	}
	
	@Override
	public ResponseABC exec() {
		try {
			String className = request.getClassName();
			if (!className.startsWith("com.codeshelf.model.domain.")) {
				className = "com.codeshelf.model.domain." + className;
			}
			UUID objectIdId = UUID.fromString(request.getPersistentId());

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				ITypedDao<IDomainObject> dao = TenantPersistenceService.getDao(classObject);
				IDomainObject object = dao.findByPersistentId(objectIdId);
				
				// First locate an instance of the parent class.

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
