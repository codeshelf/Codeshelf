package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.PropertyName;
import com.gadgetworks.codeshelf.filter.Filter;
import com.gadgetworks.codeshelf.model.dao.DaoProvider;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.ObjectGetterWsRespCmd;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterFilterRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectGetRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.RegisterFilterResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectGetResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.google.inject.Inject;
import com.google.inject.name.Named;


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
	private List<ITypedDao<IDomainObject>> daoList;
	
	public RegisterFilterCommand(RegisterFilterRequest request) {
		this.request = request;
		daoList = new ArrayList<ITypedDao<IDomainObject>>();
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

			/*
			private Class<IDomainObject>			mPersistenceClass;
			private List<IDomainObject>				mObjectMatchList;
			private List<String>					mPropertyNames;
			private String							mFilterClause;
			private Map<String, Object>				mFilterParams;
			private IDaoProvider					mDaoProvider;
			private List<ITypedDao<IDomainObject>>	mDaoList;
			*/
			
			// extract property map
			HashMap<String, Object> mFilterParams = new HashMap<String, Object>();
			for (Map<String, Object> map : filterParams) {
				String name = (String) map.get("name");
				Object value = map.get("value");
				mFilterParams.put(name, value);
			}

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				ITypedDao<IDomainObject> dao = daoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				List<IDomainObject> objectMatchList = dao.findByFilter(filterClause, mFilterParams);
				daoList.add(dao);

				// create and register filter
				Filter filter = new Filter();
				filter.setPropertyNames(propertyNames);
				
				// generate results from properties
				List<Map<String, Object>> results = filter.getProperties(objectMatchList, IWsReqCmd.OP_TYPE_UPDATE);
				response.setResults(results);
				return response;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to execute object filter command", e);
		}
		return response;
	}

}
