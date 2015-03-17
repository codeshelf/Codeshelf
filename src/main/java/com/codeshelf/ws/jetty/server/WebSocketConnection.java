package com.codeshelf.ws.jetty.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import com.codeshelf.filter.ObjectEventListener;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.IDaoListener;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;

public class WebSocketConnection implements IDaoListener {
	public enum State {
		ACTIVE,
		IDLE_WARNING,
		INACTIVE,
		CLOSED
	};

	private static final Logger					LOGGER						= LoggerFactory.getLogger(WebSocketConnection.class);

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

	private ConcurrentMap<String, ObjectEventListener>	eventListeners				= new ConcurrentHashMap<String, ObjectEventListener>();

	private ExecutorService						executorService;

	public WebSocketConnection(Session session, ExecutorService sharedExecutor) {
		this.wsSession = session;
		this.executorService = sharedExecutor;
	}

	public void sendMessage(final MessageABC message) {
    	//CodeshelfSecurityManager.setCurrentUser(this.getUser());
		try {
			if (this.wsSession != null) {
				this.wsSession.getBasicRemote().sendObject(message);
				this.messageSent();
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to send message: {}", e.getMessage());
		} finally {
	    	//CodeshelfSecurityManager.removeCurrentUser();
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
		if(executorService.isShutdown()) {
			LOGGER.warn("objectAdded called after executorService shutdown");
			return;
		}
		this.executorService.submit(new Runnable() {

			@Override
			public void run() {
				try {
					TenantPersistenceService.getInstance().beginTransaction();
					List<MessageABC> responses = new ArrayList<MessageABC>();
					synchronized(eventListeners) {
						Collection<ObjectEventListener> listeners = eventListeners.values();
						for (ObjectEventListener listener : listeners) {
							MessageABC response = listener.processObjectAdd(domainClass, domainPersistentId);
							if (response != null) {
								responses.add(response);
							}
						}
					}
					for(MessageABC response : responses) {
						sendMessage(response);
					}
					TenantPersistenceService.getInstance().commitTransaction();
				} catch (Exception e) {
					TenantPersistenceService.getInstance().rollbackTransaction();
					LOGGER.error("Unable to handle object add messages", e);
				}
			}
		});
	}

	@Override
	public void objectUpdated(final Class<? extends IDomainObject> domainClass,
		final UUID domainPersistentId,
		final Set<String> inChangedProperties) {

		if(executorService.isShutdown()) {
			LOGGER.warn("objectUpdated called after executorService shutdown");
			return;
		}
		this.executorService.submit(new Runnable() {

			@Override
			public void run() {
				try {
					TenantPersistenceService.getInstance().beginTransaction();
					List<MessageABC> responses = new ArrayList<MessageABC>();
					synchronized(eventListeners) {
						Collection<ObjectEventListener> listeners = eventListeners.values();
						for (ObjectEventListener listener : listeners) {
							MessageABC response = listener.processObjectUpdate(domainClass, domainPersistentId, inChangedProperties);
							if (response != null) {
								responses.add(response);
							}

						}
					}
					for(MessageABC response : responses) {
						sendMessage(response);						
					}
					TenantPersistenceService.getInstance().commitTransaction();
				} catch (Exception e) {
					TenantPersistenceService.getInstance().rollbackTransaction();
					LOGGER.error("Unable to handle object update", e);
				}
			}
		});
	}

	@Override
	public void objectDeleted(final Class<? extends IDomainObject> domainClass, final UUID domainPersistentId,
			final Class<? extends IDomainObject> parentClass, final UUID parentId) {
		if(executorService.isShutdown()) {
			LOGGER.warn("objectDeleted called after executorService shutdown");
			return;
		}
		this.executorService.submit(new Runnable() {

			@Override
			public void run() {
				try {
					TenantPersistenceService.getInstance().beginTransaction();
					List<MessageABC> responses = new ArrayList<MessageABC>();
					synchronized(eventListeners) {
						Collection<ObjectEventListener> listeners = eventListeners.values();
						for (ObjectEventListener listener : listeners) {
							MessageABC response = listener.processObjectDelete(domainClass, domainPersistentId, parentClass, parentId);
							if (response != null) {
								responses.add(response);
							}
						}
					}
					for(MessageABC response : responses) {
						sendMessage(response);
					}
					TenantPersistenceService.getInstance().commitTransaction();
				} catch (Exception e) {
					TenantPersistenceService.getInstance().rollbackTransaction();
					LOGGER.error("Unable to handle object delete", e);
				}
			}
		});

	}

	public void registerObjectEventListener(ObjectEventListener listener) {
		if(executorService.isShutdown()) {
			LOGGER.warn("registerObjectEventListener called after executorService shutdown");
			return;
		}
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
		/*
		List<Runnable> executorPendingTasks = this.executorService.shutdownNow();
		LOGGER.warn("when stopping UserSession executor, tasks were pending: ",executorPendingTasks.toString());
		try {
			this.executorService.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("timeout trying to stop UserSession executor");
		}
		*/

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
		// don't try to unregister listeners if we are shutting down
		if(TenantPersistenceService.getMaybeRunningInstance().state().equals(com.google.common.util.concurrent.Service.State.RUNNING)) {
			//TODO these are registered by RegisterListenerCommands. This dependency should be inverted
			TenantPersistenceService.getInstance().getEventListenerIntegrator().getChangeBroadcaster().unregisterDAOListener(this);
			
		}
	}

	public void authenticated(User user) {
		this.user = user;
		if (isSiteController()) {
			pingTimer = MetricsService.getInstance().createTimer(MetricsGroup.WSS, "ping-" + user.getUsername());
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
	/*
	public Tenant getTenant() {
		if(this.user == null)
			return null;
		return this.user.getTenant();
	}*/
}