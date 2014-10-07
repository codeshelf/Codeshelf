package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.CloseReason;
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
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.google.common.base.Objects;

public class CsSession implements IDaoListener {
	
	public enum State {
		ACTIVE,
		IDLE_WARNING,
		INACTIVE
	};

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsSession.class);

	@Getter @Setter
	String sessionId;
	
	@Getter
	Date sessionStart = new Date();
	
	@Getter @Setter
	User user;

	@Getter @Setter
	SessionType type = SessionType.Unknown;

	@Getter
	private Session	session=null;
	
	@Getter @Setter
	long lastPongReceived = 0;
	
	@Getter @Setter
	long lastMessageSent = System.currentTimeMillis();
	
	@Getter @Setter
	long lastMessageReceived = System.currentTimeMillis();
	
	@Getter @Setter
	State lastState = State.INACTIVE;
	
	@Getter @Setter
	String organizationName;
	
	// used to be new HashMap
	private Map<String,ObjectEventListener> eventListeners = new ConcurrentHashMap<String,ObjectEventListener>();
	
	private Set<ITypedDao<IDomainObject>> daoList = new ConcurrentHashSet<ITypedDao<IDomainObject>>();
	
	private ExecutorService messageSender;
	
	public CsSession(Session session) {
		this.session = session;
		this.messageSender = Executors.newSingleThreadExecutor();
	}

	public void sendMessage(final MessageABC response) {
		messageSender.execute(new Runnable() {
			@Override
			public void run() {
				try {
					session.getBasicRemote().sendObject(response);
					CsSession.this.messageSent();
				} catch (Exception e) {
					LOGGER.error("Failed to send message", e);
				}
			}
		});
	}
	
	@Override
	public void objectAdded(IDomainObject inDomainObject) {
		for (ObjectEventListener listener : eventListeners.values()) {
			MessageABC response = listener.processObjectAdd(inDomainObject);
			if (response != null) {
				sendMessage(response);
			}
		}		
	}

	@Override
	public void objectUpdated(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		for (ObjectEventListener listener : eventListeners.values()) {
			MessageABC response = listener.processObjectUpdate(inDomainObject, inChangedProperties);
			if (response != null) {
				sendMessage(response);
			}
		}
	}

	@Override
	public void objectDeleted(IDomainObject inDomainObject) {
		for (ObjectEventListener listener : eventListeners.values()) {
			MessageABC response = listener.processObjectDelete(inDomainObject);
			if (response != null) {
				sendMessage(response);
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
		if (dao==null) {
			LOGGER.warn("Can't register undefined DAO as event listener");
			return;
		}
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
		List<Runnable> unsentMessages = this.messageSender.shutdownNow();
		LOGGER.debug("Closing session with " + unsentMessages.size() + " unsent messages");
	}

	public void messageReceived() {
		this.lastMessageReceived = System.currentTimeMillis();
	}

	public void messageSent() {
		this.lastMessageSent = System.currentTimeMillis();
	}

	public void disconnect(CloseReason reason) {
		try {
			this.session.close(reason);
		} catch (Exception e) {
			LOGGER.error("Failed to close session", e);
		}
	}
	
	public String toString() {
		return Objects.toStringHelper(this)
			.add("sessionId", this.sessionId)
			.toString();
	}

}
