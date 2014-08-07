package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.ObjectEventListener;
import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class CsSession implements IDaoListener {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsSession.class);

	@Getter @Setter
	String sessionId;
	
	@Getter
	Date sessionStart = new Date();
	
	@Getter @Setter
	boolean isAuthenticated = false;

	@Getter @Setter
	SessionType type = SessionType.Undefined;

	@Getter
	private Session	session=null;
	
	@Getter @Setter
	long lastPongReceived = 0;
	
	private Map<String,ObjectEventListener> eventListeners = new HashMap<String,ObjectEventListener>();
	
	private Set<ITypedDao<IDomainObject>> daoList = new ConcurrentHashSet<ITypedDao<IDomainObject>>();
	
	public CsSession(Session session) {
		this.session = session;
	}

	private void sendResponse(ResponseABC response) {
		try {
			session.getBasicRemote().sendObject(response);
		} catch (Exception e) {
			LOGGER.error("Failed to send response", e);
		}
	}
	
	@Override
	public void objectAdded(IDomainObject inDomainObject) {
		for (ObjectEventListener listener : eventListeners.values()) {
			ResponseABC response = listener.processObjectAdd(inDomainObject);
			if (response != null) {
            	sendResponse(response);
			}
		}		
	}

	@Override
	public void objectUpdated(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		for (ObjectEventListener listener : eventListeners.values()) {
			ResponseABC response = listener.processObjectUpdate(inDomainObject, inChangedProperties);
			if (response != null) {
            	sendResponse(response);
			}
		}
	}

	@Override
	public void objectDeleted(IDomainObject inDomainObject) {
		for (ObjectEventListener listener : eventListeners.values()) {
			ResponseABC response = listener.processObjectDelete(inDomainObject);
			if (response != null) {
            	sendResponse(response);
			}
		}		
	}
	
	public void registerObjectEventListener(ObjectEventListener listener) {
		String listenerId = listener.getId();
		if (this.eventListeners.containsKey(listenerId)) {
			LOGGER.warn("Event listener "+listenerId+" already registered");
		}
		this.eventListeners.put(listenerId,listener);
		LOGGER.debug("Event listener "+listenerId+" registered: "+listener);
	}

	public void registerAsDAOListener(ITypedDao<IDomainObject> dao) {
		if (!daoList.contains(dao)) {
			dao.registerDAOListener(this);
			LOGGER.debug("Registered session "+this.sessionId+" with "+dao.getClass().getSimpleName());
			this.daoList.add(dao);
		}
	}

	public void unregisterAsDAOListener() {
		for (ITypedDao<IDomainObject> dao : this.daoList) {
			dao.unregisterDAOListener(this);
			LOGGER.debug("Unregistered session "+this.sessionId+" from "+dao.getClass().getSimpleName());
		}
	}
	
	public void close() {
		this.unregisterAsDAOListener();
	}

}
