package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.request.ObjectDeleteRequest;
import com.codeshelf.ws.protocol.response.ObjectDeleteResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("ux:delete")
public class ObjectDeleteCommand extends CommandABC<ObjectDeleteRequest> {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectDeleteCommand.class);
	
	public ObjectDeleteCommand(WebSocketConnection connection, ObjectDeleteRequest request) {
		super(connection, request);
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
			@SuppressWarnings("unchecked")
			Class<? extends IDomainObject> classObject = (Class<? extends IDomainObject>) Class.forName(className);

			if (IDomainObject.class.isAssignableFrom(classObject)) {
				ITypedDao<? extends IDomainObject> dao = TenantPersistenceService.getInstance().getDao(classObject);
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
