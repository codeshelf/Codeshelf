package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.Filter;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterFilterRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.RegisterFilterResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

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
	
	public RegisterFilterCommand(CsSession session, RegisterFilterRequest request) {
		super(session);
		this.request = request;
	}
	
	@Override
	public ResponseABC exec() {
		RegisterFilterResponse response = new RegisterFilterResponse();
		
		// CRITICAL SECURITYY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {
			String objectClassName = request.getClassName();
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.domain." + objectClassName;
			}
			List<String> propertyNames = request.getPropertyNames();
			String filterClause = request.getFilterClause();
			List<Map<String, Object>> filterParams = request.getFilterParams();
			
			// extract property map
			HashMap<String, Object> processedParams = new HashMap<String, Object>();
			for (Map<String, Object> map : filterParams) {
				String name = (String) map.get("name");
				Object value = map.get("value");
				processedParams.put(name, value);
			}

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				ITypedDao<IDomainObject> dao = daoProvider.getDaoInstance((Class<IDomainObject>) classObject);	
				this.session.registerAsDAOListener(dao);

				// moved to filter
				// List<IDomainObject> objectMatchList = dao.findByFilter(filterClause, processedParams);

				// create and register filter
				Filter filter = new Filter((Class<IDomainObject>) classObject);
				filter.setPropertyNames(propertyNames);
				filter.setFilterClause(filterClause);
				filter.setFilterParams(processedParams);
				filter.setDao(dao);
				filter.setId(request.getMessageId());
				this.session.registerObjectEventListener(filter);
				
				// generate results from properties
				List<Map<String, Object>> results = filter.getProperties(IWsReqCmd.OP_TYPE_UPDATE);
				if (results==null) {
					// don't sent a response, if there is no data
					return null;
				}
				response.setResults(results);
				return response;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to execute object filter command", e);
		}
		return response;
	}

}
