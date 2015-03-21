package com.codeshelf.ws.jetty.protocol.command;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.Organization;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.ws.jetty.protocol.request.ObjectGetRequest;
import com.codeshelf.ws.jetty.protocol.response.ObjectGetResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class ObjectGetCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectGetCommand.class);

	private ObjectGetRequest request;
	
	public ObjectGetCommand(WebSocketConnection connection, ObjectGetRequest request) {
		super(connection);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		ObjectGetResponse response = new ObjectGetResponse();
		try {
			String parentClassName = request.getClassName();
			String getterMethodName = request.getGetterMethod();
			if(parentClassName.equals("Organization")) {
				// TODO: remove UX calls to defunct Organization 
				return doGetCommand(new Organization(), getterMethodName);
			} else {
				// not an Organization
				UUID parentId = UUID.fromString(request.getPersistentId());
				if (!parentClassName.startsWith("com.codeshelf.model.domain.")) {
					parentClassName = "com.codeshelf.model.domain." + parentClassName;
				}
	
				// First we find the parent object (by it's ID).
				@SuppressWarnings("unchecked")
				Class<? extends IDomainObject> classObject = (Class<? extends IDomainObject>) Class.forName(parentClassName);
				if (IDomainObject.class.isAssignableFrom(classObject)) {				
					ITypedDao<? extends IDomainObject> dao = TenantPersistenceService.getInstance().getDao(classObject);				
					IDomainObject parentObject = dao.findByPersistentId(getTenant(),parentId);
					
					return doGetCommand(parentObject, getterMethodName);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to execute ObjectGetCommand", e);
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

	private ResponseABC doGetCommand(Object parentObject, String getterMethodName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ObjectGetResponse response = new ObjectGetResponse();
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
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
