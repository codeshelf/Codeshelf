package com.codeshelf.ws.protocol.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.filter.EventType;
import com.codeshelf.filter.Filter;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.request.RegisterFilterRequest;
import com.codeshelf.ws.protocol.response.ObjectChangeResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

/*
	Example Message:
	
	"ObjectFilterRequest":{
		"className":"Che",
		"propertyNames":["domainId","persistentId","deviceGuidStr","currentWorkArea","currentUser","lastBatteryLevel","description"],
		"filterClause":"parent.parent.persistentId = :theId",
		"filterParams":[{"name":"theId","value":"664e2fa0-00b8-11e4-ba3a-48d705ccef0f"}],
		"messageId":"cid_7"}
	}
	
*/

@RequiresPermissions("ux:filter")
public class RegisterFilterCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(RegisterFilterCommand.class);

	private RegisterFilterRequest request;

	private ObjectChangeBroadcaster	objectChangeBroadcaster;

	public RegisterFilterCommand(WebSocketConnection session, RegisterFilterRequest request, ObjectChangeBroadcaster objectChangeBroadcaster) {
		super(session);
		this.request = request;
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}
	
	@Override
	public ResponseABC exec() {
		try {
			String objectClassName = request.getClassName();
			if (!objectClassName.startsWith("com.codeshelf.model.domain.")) {
				objectClassName = "com.codeshelf.model.domain." + objectClassName;
			}
			List<Map<String, Object>> filterParams = request.getFilterParams();
			
			// extract property map
			//List<Criterion> processedParams = new ArrayList<Criterion>();
			HashMap<String, Object> processedParams = new HashMap<String, Object>();
			for (Map<String, Object> map : filterParams) {
				String name = (String) map.get("name");
				Object value = map.get("value");
				processedParams.put(name, value);
				//processedParams.add(Restrictions.eq(name, value));
			}

			// First we find the object (by it's ID).
			@SuppressWarnings("unchecked")
			Class<? extends IDomainObject> classObject = (Class<? extends IDomainObject>) Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				this.objectChangeBroadcaster.registerDAOListener(wsConnection.getCurrentTenantIdentifier(), wsConnection, classObject);

				ITypedDao<? extends IDomainObject> dao = TenantPersistenceService.getInstance().getDao(classObject);
				// create listener
				
				String filterClause = request.getFilterClause();
					
				Filter filter = new Filter(dao, classObject, request.getMessageId());				
				filter.setPropertyNames(request.getPropertyNames());
				filter.setParams(processedParams);
				filter.setCriteriaName(filterClause);
				List<? extends IDomainObject> objectMatchList = filter.refreshMatchList();
				
				// DEV-1085  If this filter "maxed out", lets not register the listener as this makes a fairly severe load on the server.
				// This means only small lists will live update.
				if (!filter.tooBigToBeEfficientLister()){
					this.wsConnection.registerObjectEventListener(filter);}
				// Note that this does not register if filter maxes out. But nothing yet deals with a filter that starts small and grows.
				else{
					LOGGER.info("filter not registered because its results are too large. The corresponding client list view will not live update");
				}
				// generate response
				List<Map<String, Object>> results = filter.getProperties(objectMatchList, EventType.Update);
				if (results==null || results.size()==0) {
					return null;
				}
				ObjectChangeResponse response = new ObjectChangeResponse();
				response.setResults(results);
				response.setStatus(ResponseStatus.Success);
				return response;
			}
			else {
				LOGGER.error("Failed to execute "+this.getClass().getSimpleName()+": "+objectClassName+" is not a domain object");
			}
		} catch (Exception e) {
			LOGGER.error("Failed to execute "+this.getClass().getSimpleName() + " for request: " + request, e);
		}
		ObjectChangeResponse response = new ObjectChangeResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
