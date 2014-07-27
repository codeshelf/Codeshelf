package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.Filter;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectListenerRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterFilterRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectListenerResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class ObjectListenerCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectGetCommand.class);

	private ObjectListenerRequest request;
	
	private List<ITypedDao<IDomainObject>> daoList;
	
	
	public ObjectListenerCommand(ObjectListenerRequest request) {
		this.request = request;
		daoList = new ArrayList<ITypedDao<IDomainObject>>();
	}

	@Override
	public ResponseABC exec() {
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
			
			List<UUID> objectIds = request.getObjectIds();
			List<String> propertyNames = request.getPropertyNames();

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				Class<IDomainObject> persistenceClass = (Class<IDomainObject>) classObject;

				ITypedDao<IDomainObject> dao = daoProvider.getDaoInstance((Class<IDomainObject>) persistenceClass);
				List<IDomainObject> objectMatchList = dao.findByPersistentIdList(objectIds);
				daoList.add(dao);

				// create and register filter
				Filter filter = new Filter();
				filter.setPropertyNames(propertyNames);
				
				List<Map<String, Object>> results = filter.getProperties(objectMatchList, IWsReqCmd.OP_TYPE_UPDATE);
				if (results==null) {
					// don't sent a response, if there is no data
					return null;
				}
				ObjectListenerResponse response = new ObjectListenerResponse();				
				response.setResults(results);
				response.setStatus(ResponseStatus.Success);
				return response;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to execute ObjectListenerCommand", e);
		}
		ObjectListenerResponse response = new ObjectListenerResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
