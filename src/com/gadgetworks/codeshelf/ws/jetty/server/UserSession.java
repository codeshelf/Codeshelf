package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.gadgetworks.codeshelf.application.ContextLogging;
import com.gadgetworks.codeshelf.filter.ObjectEventListener;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.UserType;
import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.gadgetworks.codeshelf.platform.multitenancy.User;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public class UserSession implements IDaoListener {
	public enum State {
		ACTIVE,
		IDLE_WARNING,
		INACTIVE,
		CLOSED
	};

	private static final Logger					LOGGER						= LoggerFactory.getLogger(UserSession.class);

	@Getter
	@Setter
	String										sessionId;

	@Getter
	User										user;

	@Getter
	Date										sessionStart				= new Date();

	@Getter
	private Session								wsSession					= null;

	@Getter
	@Setter
	long										lastPingSent				= 0;

	@Getter
	@Setter
	long										lastPondRoundtripDuration	= 0;

	@Getter
	@Setter
	long										lastMessageSent				= System.currentTimeMillis();

	@Getter
	@Setter
	long										lastMessageReceived			= System.currentTimeMillis();

	@Getter
	@Setter
	State										lastState					= State.ACTIVE;

	private Timer								pingTimer					= null;

	private Map<String, ObjectEventListener>	eventListeners				= new ConcurrentHashMap<String, ObjectEventListener>();

	private ExecutorService						executorService;

	public UserSession(Session session, ExecutorService sharedExecutor) {
		this.wsSession = session;
		this.executorService = sharedExecutor;

	}

	public void sendMessage(final MessageABC message) {
		ContextLogging.setSession(UserSession.this);
		try {
			if (this.wsSession != null) {
				this.wsSession.getBasicRemote().sendObject(message);
				this.messageSent();
			}
		} catch (Exception e) {
			LOGGER.error("Failed to send message", e);
		} finally {
			ContextLogging.clearSession();
		}
	}

	public boolean isAuthenticated() {
		return (user != null);
	}

	public boolean isSiteController() {
		if (!this.isAuthenticated())
			return false;
		return this.user.getType().equals(UserType.SITECON);
	}

	public boolean isAppUser() {
		if (!this.isAuthenticated())
			return false;
		return this.user.getType().equals(UserType.APPUSER);
	}

	@Override
	public void objectAdded(final Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		this.executorService.submit(new Runnable() {

			@Override
			public void run() {
				try {
					PersistenceService.getInstance().beginTenantTransaction();
					for (ObjectEventListener listener : eventListeners.values()) {
						MessageABC response = listener.processObjectAdd(domainClass, domainPersistentId);
						if (response != null) {
							sendMessage(response);
						}
					}
					PersistenceService.getInstance().commitTenantTransaction();
				} catch (Exception e) {
					PersistenceService.getInstance().rollbackTenantTransaction();
					LOGGER.error("Unable to handle object add messages", e);
				}
			}
		});
	}

	@Override
	public void objectUpdated(final Class<? extends IDomainObject> domainClass,
		final UUID domainPersistentId,
		final Set<String> inChangedProperties) {
		this.executorService.submit(new Runnable() {

			@Override
			public void run() {
				try {
					PersistenceService.getInstance().beginTenantTransaction();
					for (ObjectEventListener listener : eventListeners.values()) {
						MessageABC response = listener.processObjectUpdate(domainClass, domainPersistentId, inChangedProperties);
						if (response != null) {
							sendMessage(response);
						}

					}
					PersistenceService.getInstance().commitTenantTransaction();
				} catch (Exception e) {
					PersistenceService.getInstance().rollbackTenantTransaction();
					LOGGER.error("Unable to handle object update", e);
				}
			}
		});
	}

	@Override
	public void objectDeleted(final Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		this.executorService.submit(new Runnable() {

			@Override
			public void run() {
				try {
					PersistenceService.getInstance().beginTenantTransaction();
					for (ObjectEventListener listener : eventListeners.values()) {
						MessageABC response = listener.processObjectDelete(domainClass, domainPersistentId);
						if (response != null) {
							sendMessage(response);
						}
					}
					PersistenceService.getInstance().commitTenantTransaction();
				} catch (Exception e) {
					PersistenceService.getInstance().rollbackTenantTransaction();
					LOGGER.error("Unable to handle object delete", e);
				}
			}
		});

	}

	public void registerObjectEventListener(ObjectEventListener listener) {
		String listenerId = listener.getId();
		if (this.eventListeners.containsKey(listenerId)) {
			LOGGER.warn("Event listener " + listenerId + " already registered");
		}
		this.eventListeners.put(listenerId, listener);
		Integer count = eventListeners.size();
		LOGGER.debug("Event listener " + listenerId + " registered: " + listener + ". " + count + " listeners total.");
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
		this.lastState = State.CLOSED;
		if (this.user != null)
			this.user = null;
		if (this.sessionId != null)
			this.sessionId = null;
		if (this.wsSession != null) {
			try {
				this.wsSession.close(reason);
			} catch (Exception e) {
				LOGGER.error("Failed to close session", e);
			}
			this.wsSession = null;
		}
		//TODO these are registered by RegisterListenerCommands. This dependency should be inverted
		PersistenceService.getInstance().getObjectChangeBroadcaster().unregisterDAOListener(this);
	}

	public void authenticated(User user) {
		this.user = user;
		if (isSiteController()) {
			this.pingTimer = MetricsService.addTimer(MetricsGroup.WSS, "ping-" + user.getTenant().getDbSchemaName() + "." + user.getUsername());
		}
	}

	public void pongReceived(long startTime) {
		long now = System.currentTimeMillis();
		long delta = now - startTime;
		if (this.pingTimer != null) {
			pingTimer.update(delta, TimeUnit.MILLISECONDS);
		}
		double elapsedSec = ((double) delta) / 1000;
		LOGGER.debug("Ping roundtrip on session " + this.sessionId + " in " + elapsedSec + "s");
	}
	
	public Tenant getTenant() {
		if(this.user == null)
			return null;
		return this.user.getTenant();
	}
}
