package com.codeshelf.ws.protocol.command;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.request.ObjectUpdateRequest;
import com.codeshelf.ws.protocol.response.ObjectUpdateResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("ux:update")
public class ObjectUpdateCommand extends CommandABC<ObjectUpdateRequest> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectUpdateCommand.class);

	private PropertyUtilsBean propertyUtil = new PropertyUtilsBean();
	
	public ObjectUpdateCommand(WebSocketConnection connection, ObjectUpdateRequest request) {
		super(connection, request);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ResponseABC exec() {
		ObjectUpdateResponse response = new ObjectUpdateResponse();

		// extract UUID
		String className = request.getClassName();
		String persistentId = request.getPersistentId();
		if (persistentId==null) {
			LOGGER.error("Failed to update "+className+":  Object ID is undefined");
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("persistentId was null");
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

		if (!className.startsWith("com.codeshelf.model.domain.")) {
			className = "com.codeshelf.model.domain." + className;
		}
		
		Map<String, Object> properties = request.getProperties();
		try {
			// First we find the parent object (by it's ID).
			Class<? extends IDomainObject> classObject = (Class<? extends IDomainObject>) Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				// First locate an instance of the parent class.				
				ITypedDao<? extends IDomainObject> dao = TenantPersistenceService.getInstance().getDao(classObject);
				
				IDomainObject updateObject = null;
				if(dao == null) {
					LOGGER.error("got null dao for "+className);
				} else {
					updateObject = dao.findByPersistentId(objectId);					
				}
				
				// Execute the "set" method against the parents to return the children.
				// (The method *must* start with "set" to ensure other methods don't get called.)
				if (updateObject != null) {
					// update object...
					Map<String, Throwable> failures = new HashMap<String, Throwable>();
					for (Map.Entry<String, Object> property : properties.entrySet()) {
						try {
							Object propertyValue = property.getValue();
							Class<?> propertyType = propertyUtil.getPropertyDescriptor(updateObject, property.getKey()).getPropertyType();
							if (Short.class.isAssignableFrom(propertyType)) {
								if (propertyValue instanceof Number) {
									propertyValue = new Short(((Number) propertyValue).shortValue());
								}
								else if (propertyValue instanceof String) {
									propertyValue = new Short((String)propertyValue);
								}
							}
							
							if(Enum.class.isAssignableFrom(propertyType)) {
								propertyValue = Enum.valueOf((Class<? extends Enum>)propertyType, propertyValue.toString());
							}
							
							propertyUtil.setProperty(updateObject, property.getKey(), propertyValue);
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
