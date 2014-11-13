package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.gadgetworks.codeshelf.application.ContextLogging;
import com.gadgetworks.codeshelf.filter.ObjectEventListener;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.UserType;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public class UserSession implements IDaoListener {
	public enum State {
		ACTIVE,
		IDLE_WARNING,
		INACTIVE,
		CLOSED
	};

	private static final Logger	LOGGER = LoggerFactory.getLogger(UserSession.class);

	@Getter @Setter
	String sessionId;

	@Getter 
	User user;
	
	@Getter
	Date sessionStart = new Date();

	@Getter
	private Session	wsSession=null;
	
	@Getter @Setter
	long lastPingSent = 0;
	
	@Getter @Setter
	long lastPondRoundtripDuration = 0;
	
	@Getter @Setter
	long lastMessageSent = System.currentTimeMillis();
	
	@Getter @Setter
	long lastMessageReceived = System.currentTimeMillis();
	
	@Getter @Setter
	State lastState = State.ACTIVE;
	
	private Timer pingTimer = null;
	
	private Map<String,ObjectEventListener> eventListeners = new ConcurrentHashMap<String,ObjectEventListener>();
	
	private Set<ITypedDao<IDomainObject>> daoList = new ConcurrentHashSet<ITypedDao<IDomainObject>>();
	
	public UserSession(Session session) {
		this.wsSession = session;
	}

	public void sendMessage(final MessageABC message) {
		ContextLogging.setSession(UserSession.this);
		try {
			this.wsSession.getBasicRemote().sendObject(message);
			this.messageSent();
		} catch (Exception e) {
			LOGGER.error("Failed to send message", e);
		} finally {
			ContextLogging.clearSession();
		}
	}
	
	public boolean isAuthenticated() {
		return (user!=null);
	}
	
	public boolean isSiteController() {
		if(!this.isAuthenticated())
			return false;
		return this.user.getType().equals(UserType.SITECON);
	}
	
	public boolean isAppUser() {
		if(!this.isAuthenticated())
			return false;
		return this.user.getType().equals(UserType.APPUSER);
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
		if (dao == null) {
			LOGGER.error("couldn't register session as listener to null dao");
		} else if (!daoList.contains(dao)) {
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
	
	public void messageReceived() {
		this.lastMessageReceived = System.currentTimeMillis();
	}

	public void messageSent() {
		this.lastMessageSent = System.currentTimeMillis();
	}
	
	public void disconnect() {
		disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Unspecified"));
	}

	public void disconnect(CloseReason reason) {
		this.unregisterAsDAOListener();
		this.lastState=State.CLOSED;
		if(this.user != null)
			this.user=null;
		if(this.sessionId != null)
			this.sessionId=null;
		if(this.wsSession != null) {
			try {
				this.wsSession.close(reason);
			} catch (Exception e) {
				LOGGER.error("Failed to close session", e);
			}
			this.wsSession=null;
		}
	}

	public void authenticated(User user) {
		this.user = user;
		if (isSiteController()) {
			String organizationName = user.getOrganization().getDomainId();
			this.pingTimer = MetricsService.addTimer(MetricsGroup.WSS,"ping-"+organizationName+"."+user.getDomainId());
		}
	}

	public void pongReceived(long startTime) {
		long now = System.currentTimeMillis();
		long delta = now-startTime;
		if (this.pingTimer!=null) {
			pingTimer.update(delta, TimeUnit.MILLISECONDS);
		}
		double elapsedSec = ((double) delta)/1000; 
		LOGGER.debug("Ping roundtrip on session "+this.sessionId +" in "+elapsedSec+"s");
	}
}