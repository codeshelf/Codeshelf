package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.EventType;
import com.gadgetworks.codeshelf.filter.Listener;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterListenerRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectChangeResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class RegisterListenerCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(RegisterListenerCommand.class);

	private RegisterListenerRequest request;

	private ObjectChangeBroadcaster	objectChangeBroadcaster;
	
	public RegisterListenerCommand(UserSession session, RegisterListenerRequest request, ObjectChangeBroadcaster objectChangeBroadcaster) {
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

			// First we find the object (by it's ID).
			Class<?> classObject = Class.forName(objectClassName);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				// register session with DAO
				this.objectChangeBroadcaster.registerDAOListener(session, (Class<IDomainObject>)classObject);

				// create listener
				Listener listener = new Listener((Class<IDomainObject>) classObject, request.getMessageId());				
				listener.setMatchList(request.getObjectIds());
				listener.setPropertyNames(request.getPropertyNames());
				this.session.registerObjectEventListener(listener);

				// generate response
				ITypedDao<IDomainObject> dao = PersistenceService.getDao(classObject);				
				List<IDomainObject> objectMatchList = dao.findByPersistentIdList(request.getObjectIds());
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
