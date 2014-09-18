package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.EventType;
import com.gadgetworks.codeshelf.filter.Listener;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterListenerRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectChangeResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class RegisterListenerCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(RegisterListenerCommand.class);

	private RegisterListenerRequest request;
	
	public RegisterListenerCommand(CsSession session, RegisterListenerRequest request) {
		super(session);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		try {
			String objectClassName = request.getClassName();
			if (!objectClassName.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				objectClassName = "com.gadgetworks.codeshelf.model.domain." + objectClassName;
			}			

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				// register session with DAO
				@SuppressWarnings("unchecked")
				Class<IDomainObject> persistenceClass = (Class<IDomainObject>) classObject;
				ITypedDao<IDomainObject> dao = daoProvider.getDaoInstance((Class<IDomainObject>) persistenceClass);
				this.session.registerAsDAOListener(dao);

				// create listener
				List<IDomainObject> objectMatchList = dao.findByPersistentIdList(request.getObjectIds());
				@SuppressWarnings("unchecked")
				Listener listener = new Listener((Class<IDomainObject>) classObject);				
				listener.setId(request.getMessageId());
				listener.setMatchList(request.getObjectIds());
				listener.setPropertyNames(request.getPropertyNames());
				this.session.registerObjectEventListener(listener);

				// generate response
				List<Map<String, Object>> results = listener.getProperties(objectMatchList, EventType.Update);
				ObjectChangeResponse response = new ObjectChangeResponse();
				response.setResults(results);
				response.setStatus(ResponseStatus.Success);
				return response;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to execute "+this.getClass().getSimpleName(), e);
		}
		ObjectChangeResponse response = new ObjectChangeResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
