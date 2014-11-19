package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.EventType;
import com.gadgetworks.codeshelf.filter.Filter;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterFilterRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectChangeResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.google.common.base.Strings;

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

public class RegisterFilterCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(RegisterFilterCommand.class);

	private RegisterFilterRequest request;

	private ObjectChangeBroadcaster	objectChangeBroadcaster;
	
	public RegisterFilterCommand(UserSession session, RegisterFilterRequest request, ObjectChangeBroadcaster objectChangeBroadcaster) {
		super(session);
		this.request = request;
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ResponseABC exec() {
		try {
			String objectClassName = request.getClassName();
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.domain." + objectClassName;
			}
			String filterClause = request.getFilterClause();
			List<Map<String, Object>> filterParams = request.getFilterParams();
			
			// extract property map
			//List<Criterion> processedParams = new ArrayList<Criterion>();
			HashMap<String, Object> processedParams = new HashMap<String, Object>();
			for (Map<String, Object> map : filterParams) {
				String name = (String) map.get("name");
				Object value = map.get("value");
				String className = (String) map.get("type");
				if (!Strings.isNullOrEmpty(className)) {
					value = convertFilterParam(className, value);
				}
				processedParams.put(name, value);
				//processedParams.add(Restrictions.eq(name, value));
			}

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				this.objectChangeBroadcaster.registerDAOListener(session, (Class<IDomainObject>)classObject);

				// create listener
				ITypedDao<IDomainObject> dao = PersistenceService.getDao(classObject);
				
				Filter filter = new Filter((Class<IDomainObject>) classObject, request.getMessageId());				
				filter.setPropertyNames(request.getPropertyNames());
				filter.setParams(processedParams);
				filter.setClause(filterClause);
				filter.setDao(dao);
				List<IDomainObject> objectMatchList = filter.refreshMatchList();
				this.session.registerObjectEventListener(filter);

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

	private Object convertFilterParam(String className, Object value) throws ClassNotFoundException {
		Class<?> classObject = Class.forName(className);
		if (java.sql.Timestamp.class.isAssignableFrom(classObject)) {
			return new java.sql.Timestamp(Long.valueOf(value.toString()));
		}
		else {
			return value;
		}
	}
	
}
