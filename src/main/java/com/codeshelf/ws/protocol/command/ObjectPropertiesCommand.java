package com.codeshelf.ws.protocol.command;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FacilityProperty;
import com.codeshelf.ws.protocol.request.ObjectPropertiesRequest;
import com.codeshelf.ws.protocol.response.ObjectPropertiesResponseNew;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("ux:get")
public class ObjectPropertiesCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectPropertiesCommand.class);

	private ObjectPropertiesRequest request;
	private PropertyBehavior propertyBehavior;
		
	public ObjectPropertiesCommand(WebSocketConnection connection, ObjectPropertiesRequest request, PropertyBehavior propertyBehavior) {
		super(connection);
		this.request = request;
		this.propertyBehavior = propertyBehavior;
	}
	
	/*
	@Override
	public ResponseABC exec() {
		String className = request.getClassName();
		String persistentId = request.getPersistentId();
		ObjectPropertiesResponse response = new ObjectPropertiesResponse(className,persistentId);

		// extract UUID
		if (persistentId==null) {
			LOGGER.error("Failed to get proerties for "+className+":  Object ID is undefined");
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("persistentId was null");
			return response;
		}
		UUID objectId = null;
		try {
			objectId = UUID.fromString(persistentId);
		}
		catch (Exception e) {
			LOGGER.error("Failed to get properties for "+className,e);
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Failed to convert object ID "+persistentId+" to UUID");
			return response;
		}

		if (!className.startsWith("com.codeshelf.model.domain.")) {
			className = "com.codeshelf.model.domain." + className;
		}
		
		try {
			// First we find the parent object (by it's ID).
			@SuppressWarnings("unchecked")
			Class<? extends IDomainObject> classObject = (Class<? extends IDomainObject>) Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				// First locate an instance of the parent class.				
				ITypedDao<? extends IDomainObject> dao = TenantPersistenceService.getInstance().getDao(classObject);
				IDomainObject object = null;
				if(dao == null) {
					LOGGER.error("DAO is undefined for "+className);
				} else {
					object = dao.findByPersistentId(objectId);					
				}				
				if (object != null) {
					// all good. pass back object properties
					List<DomainObjectProperty> props = PropertyDao.getInstance().getPropertiesWithDefaults(object);
					// we do not want to just serialize the DomainObjectProperty persistent fields. We need to call methods to get the meta-fields					
					// prop is a list of DomainObjectProperty. So setProperties(props) is merely a Json string of the persistent fields.
					// unless there is a getProperties override
					List<Map<String, Object>> results = response.getPropertyResults(props);
					response.setResults(results);
					// response.setProperties(props);
					
					response.setStatus(ResponseStatus.Success);
					return response;
				}
				else {
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage(className+" with ID #"+objectId+" not found");
					return response;
				}
			}
		}
		catch (Exception e) {
			LOGGER.error("Failed to get properties for object "+className+"("+objectId+")",e);
		}
		response.setStatus(ResponseStatus.Fail);
		response.setStatusMessage("Unable to retrieve object properties");
		return response;
	}
	*/
	
	@Override
	public ObjectPropertiesResponseNew exec() {
		ObjectPropertiesResponseNew response = new ObjectPropertiesResponseNew();
		String persistentId = request.getPersistentId();
		Facility facility = Facility.staticGetDao().findByPersistentId(persistentId);
		if (facility != null) {
			List<FacilityProperty> properties = propertyBehavior.getAllProperties(facility);
			response.setResults(properties);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Unable to find Facility {} to retrieve its properties", persistentId);
		response.setStatus(ResponseStatus.Fail);
		response.setStatusMessage("Unable to find Facility " + persistentId + " to retrieve its properties");
		return response;
	}
	
}
