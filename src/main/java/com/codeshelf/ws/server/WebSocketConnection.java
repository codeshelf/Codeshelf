package com.codeshelf.ws.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

import org.apache.mina.util.ConcurrentHashSet;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codeshelf.filter.ObjectEventListener;
import com.codeshelf.manager.Tenant;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.IDaoListener;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.message.CheStatusMessage;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.google.common.util.concurrent.Service;

public class WebSocketConnection implements IDaoListener {
	public enum State {
		ACTIVE,
		IDLE_WARNING,
		INACTIVE,
		CLOSED
	};

	private static final Logger							LOGGER							= LoggerFactory.getLogger(WebSocketConnection.class);

	@Getter
	@Setter
	String												sessionId;

	@Getter
	UserContext											currentUserContext;

	@Getter
	Tenant												currentTenant;

	@Getter
	String												lastTenantIdentifier;																	// even after disconnection

	@Getter
	Date												sessionStart					= new Date();

	@Getter
	private Session										wsSession						= null;

	@Getter
	@Setter
	long												lastPingSent					= 0;

	@Getter
	@Setter
	long												lastPondRoundtripDuration		= 0;

	@Getter
	@Setter
	long												lastMessageSent					= System.currentTimeMillis();

	@Getter
	@Setter
	long												nextPutWallRefresh				= -1;

	@Getter
	@Setter
	long												putwallUpdateInterval			= 12 * 60 * 60 * 1000;									// 12 hours
	//long putwallUpdateInterval = 60 * 1000; // every min

	@Getter
	@Setter
	long												nextCheRefresh					= -1;

	@Getter
	@Setter
	long												initialUpdateDelay				= 1000 * 10;											// send updates 10 sec after authentication

	@Getter
	@Setter
	long												updateFailureDelay				= 60 * 1000;											// try again in a minute

	@Getter
	@Setter
	long												lastMessageReceived				= System.currentTimeMillis();

	@Getter
	@Setter
	State												lastState						= State.ACTIVE;

	private Timer										pingTimer						= null;

	private ConcurrentMap<String, ObjectEventListener>	eventListeners					= new ConcurrentHashMap<String, ObjectEventListener>();

	private ExecutorService								executorService;

	WorkService											workService;

	// track individual tasks
	Set<Future<?>>										pendingFutures					= new ConcurrentHashSet<Future<?>>();

	private static final int							NUM_FUTURES_CLEANUP_THRESHOLD	= 5;

	public WebSocketConnection(Session session, ExecutorService sharedExecutor, WorkService workService) {
		this.wsSession = session;
		this.workService = workService;
		this.executorService = sharedExecutor;
	}

	public String getCurrentTenantIdentifier() {
		if (this.currentTenant == null)
			return null;
		return this.currentTenant.getTenantIdentifier();
	}

	public boolean sendMessage(final MessageABC message) {
		boolean sent = false;
		try {
			if (this.wsSession != null) {
				this.wsSession.getBasicRemote().sendObject(message);
				this.messageSent();
				sent = true;
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to send " + message.getClass().getSimpleName() + " message on session {}: {}",
				this.getSessionId(),
				e.getMessage());
		} catch (WebSocketException e) {
			LOGGER.warn("Failed to send " + message.getClass().getSimpleName() + " message on session {}: {}",
				this.getSessionId(),
				e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Unexpected exception encoding/sending " + message.getClass().getSimpleName() + " message", e);
		}
		return sent;
	}

	public boolean isAuthenticated() {
		return (currentUserContext != null);
	}

	@Override
	public void objectAdded(final Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		if (executorService.isShutdown()) {
			LOGGER.warn("objectAdded called after executorService shutdown: {} {} {}",
				domainClass.getSimpleName(),
				domainPersistentId,
				this.getSessionId());
			return;
		}
		this.cleanupFuturesList();
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		if (tenant == null) {
			LOGGER.error("null tenant context trying to notify on add object {} {}",
				domainClass.getSimpleName(),
				domainPersistentId);
		} else if (!tenant.equals(this.currentTenant)) {
			LOGGER.error("inconsistent tenant context {} (expected {}) trying to notify on add object {} {}",
				tenant,
				this.currentTenant,
				domainClass.getSimpleName(),
				domainPersistentId);
		} else {
			Future<?> future = this.executorService.submit(new Runnable() {

				@Override
				public void run() {
					CodeshelfSecurityManager.removeContextIfPresent();
					CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), currentTenant);
					try {
						TenantPersistenceService.getInstance().beginTransaction();
						List<MessageABC> responses = new ArrayList<MessageABC>();
						synchronized (eventListeners) {
							Collection<ObjectEventListener> listeners = eventListeners.values();
							for (ObjectEventListener listener : listeners) {
								MessageABC response = listener.processObjectAdd(domainClass, domainPersistentId);
								if (response != null) {
									responses.add(response);
								}
							}
						}
						for (MessageABC response : responses) {
							sendMessage(response);
						}
						TenantPersistenceService.getInstance().commitTransaction();
					} catch (Exception e) {
						TenantPersistenceService.getInstance().rollbackTransaction();
						LOGGER.error("Unable to handle object add messages", e);
					} finally {
						CodeshelfSecurityManager.removeContext();
					}
				}
			});
			this.pendingFutures.add(future);
		}
	}

	@Override
	public void objectUpdated(final Class<? extends IDomainObject> domainClass,
		final UUID domainPersistentId,
		final Set<String> inChangedProperties) {

		if (executorService.isShutdown()) {
			LOGGER.warn("objectUpdated called after executorService shutdown");
			return;
		}
		this.cleanupFuturesList();
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		if (tenant == null) {
			LOGGER.error("null tenant context trying to notify on update object {} {}",
				domainClass.getSimpleName(),
				domainPersistentId);
		} else if (!tenant.equals(this.currentTenant)) {
			LOGGER.error("inconsistent tenant context {} (expected {}) trying to notify on update object {} {}",
				tenant,
				this.currentTenant,
				domainClass.getSimpleName(),
				domainPersistentId);
		} else {
			Future<?> future = this.executorService.submit(new Runnable() {

				@Override
				public void run() {
					CodeshelfSecurityManager.removeContextIfPresent();
					CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), currentTenant);
					try {
						TenantPersistenceService.getInstance().beginTransaction();
						List<MessageABC> responses = new ArrayList<MessageABC>();
						synchronized (eventListeners) {
							Collection<ObjectEventListener> listeners = eventListeners.values();
							for (ObjectEventListener listener : listeners) {
								MessageABC response = listener.processObjectUpdate(domainClass,
									domainPersistentId,
									inChangedProperties);
								if (response != null) {
									responses.add(response);
								}

							}
						}
						for (MessageABC response : responses) {
							sendMessage(response);
						}
						TenantPersistenceService.getInstance().commitTransaction();
					} catch (Exception e) {
						TenantPersistenceService.getInstance().rollbackTransaction();
						LOGGER.error("Unable to handle object update", e);
					} finally {
						CodeshelfSecurityManager.removeContext();
					}
				}
			});
			this.pendingFutures.add(future);
		}
	}

	@Override
	public void objectDeleted(final Class<? extends IDomainObject> domainClass,
		final UUID domainPersistentId,
		final Class<? extends IDomainObject> parentClass,
		final UUID parentId) {
		if (executorService.isShutdown()) {
			LOGGER.warn("objectDeleted called after executorService shutdown");
			return;
		}
		this.cleanupFuturesList();
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		if (tenant == null) {
			LOGGER.error("null tenant context trying to notify on deleted object {} {}",
				domainClass.getSimpleName(),
				domainPersistentId);
		} else if (!tenant.equals(this.currentTenant)) {
			LOGGER.error("inconsistent tenant context {} (expected {}) trying to notify on deleted object {} {}",
				tenant,
				this.currentTenant,
				domainClass.getSimpleName(),
				domainPersistentId);
		} else {
			Future<?> future = this.executorService.submit(new Runnable() {

				@Override
				public void run() {
					CodeshelfSecurityManager.removeContextIfPresent();
					CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), currentTenant);
					try {
						TenantPersistenceService.getInstance().beginTransaction();
						List<MessageABC> responses = new ArrayList<MessageABC>();
						synchronized (eventListeners) {
							Collection<ObjectEventListener> listeners = eventListeners.values();
							for (ObjectEventListener listener : listeners) {
								MessageABC response = listener.processObjectDelete(domainClass,
									domainPersistentId,
									parentClass,
									parentId);
								if (response != null) {
									responses.add(response);
								}
							}
						}
						for (MessageABC response : responses) {
							sendMessage(response);
						}
						TenantPersistenceService.getInstance().commitTransaction();
					} catch (Exception e) {
						TenantPersistenceService.getInstance().rollbackTransaction();
						LOGGER.error("Unable to handle object delete", e);
					} finally {
						CodeshelfSecurityManager.removeContext();
					}
				}
			});
			this.pendingFutures.add(future);
		}
	}

	public void registerObjectEventListener(ObjectEventListener listener) {
		if (executorService.isShutdown()) {
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
		 * can't do this because the executor is shared and cannot be restarted
		 * 
		List<Runnable> executorPendingTasks = this.executorService.shutdownNow();
		LOGGER.warn("when stopping UserSession executor, tasks were pending: ",executorPendingTasks.toString());
		try {
			this.executorService.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("timeout trying to stop UserSession executor");
		}
		*/
		// wait for all listener threads to stop
		this.cancelFutures();

		this.lastState = State.CLOSED;
		this.currentUserContext = null;
		this.currentTenant = null;
		this.sessionId = null;
		if (this.wsSession != null) {
			try {
				this.wsSession.close(reason);
			} catch (Exception e) {
				LOGGER.error("Failed to close session", e);
			}
			this.wsSession = null;
		}
		// don't try to unregister listeners if service is shutting down, or if there's never been an authenticated user
		if (TenantPersistenceService.getMaybeRunningInstance().state().equals(Service.State.RUNNING)
				|| this.lastTenantIdentifier == null) {
			//TODO these are registered by RegisterListenerCommands. This dependency should be inverted
			ObjectChangeBroadcaster ocb = TenantPersistenceService.getInstance()
				.getEventListenerIntegrator()
				.getChangeBroadcaster();
			ocb.unregisterDAOListener(this.lastTenantIdentifier, this);
		}
	}

	private synchronized void cleanupFuturesList() {
		int numFutures = this.pendingFutures.size();
		if (numFutures > NUM_FUTURES_CLEANUP_THRESHOLD) {
			ConcurrentHashSet<Future<?>> remainingFutures = new ConcurrentHashSet<Future<?>>();
			for (Future<?> future : this.pendingFutures) {
				if (!future.isDone())
					remainingFutures.add(future);
			}
			this.pendingFutures = remainingFutures;
		} // else don't bother
	}

	private void cancelFutures() {
		Future<?>[] futures = new Future<?>[this.pendingFutures.size()];
		for (Future<?> future : this.pendingFutures.toArray(futures)) {
			if (!future.isDone()) {
				future.cancel(true);
			}
		}
		this.pendingFutures.clear();
	}

	public void authenticated(UserContext user, Tenant tenant) {
		if (user == null)
			throw new NullPointerException("authenticated user may not be null");
		if (tenant == null)
			throw new NullPointerException("authenticated tenant may not be null");
		this.currentUserContext = user;
		this.currentTenant = tenant;
		this.lastTenantIdentifier = tenant.getTenantIdentifier();
		if (user.isSiteController()) {
			pingTimer = MetricsService.getInstance().createTimer(MetricsGroup.WSS, "ping-" + user.getUsername());
		}
		// set target time for initial che update
		this.nextCheRefresh = System.currentTimeMillis() + initialUpdateDelay;
		this.nextPutWallRefresh = System.currentTimeMillis() + initialUpdateDelay;
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

	public void close() throws IOException {
		Session session = getWsSession();
		if (session != null) {
			session.close(new CloseReason(CloseCodes.GOING_AWAY, ""));
		}
	}

	public void checkForCheUpdates() {
		if (this.getCurrentUserContext() == null) {
			LOGGER.info("Failed to send Che updates to site controller: User context is undefined.");
			return;
		}
		if (getNextCheRefresh() >= 0 && System.currentTimeMillis() > getNextCheRefresh()) {
			try {
				TenantPersistenceService.getInstance().beginTransaction();
				SiteController siteController = SiteController.staticGetDao().findByDomainId(null,
					this.currentUserContext.getUsername());
				CodeshelfNetwork network = siteController.getParent();
				List<Che> ches = Che.staticGetDao().findByParent(network);
				if (ches != null) {
					LOGGER.info("Sending " + ches.size() + " Che updates to site controller "
							+ this.getCurrentUserContext().getUsername());
					for (Che che : ches) {
						CheStatusMessage msg = new CheStatusMessage(che);
						this.sendMessage(msg);
					}
				}
				// this.nextCheRefresh = System.currentTimeMillis() + this.cheUpdateInterval;
				this.nextCheRefresh = -1; // do not refresh periodically
				TenantPersistenceService.getInstance().commitTransaction();
			} catch (Exception e) {
				LOGGER.error("Failed to send che updates to site controller " + this.getCurrentUserContext().getUsername(), e);
				this.setNextCheRefresh(System.currentTimeMillis() + updateFailureDelay);

			} finally {
				TenantPersistenceService.getInstance().rollbackAnyActiveTransactions();
			}
		}
	}

	public void checkForPutWallUpdate() {
		if (this.getCurrentUserContext() == null) {
			LOGGER.info("Failed to send PutWall update to site controller: User context is undefined.");
			return;
		}
		long startMillis = System.currentTimeMillis();
		if (getNextPutWallRefresh() >= 0 && startMillis > getNextPutWallRefresh()) {
			try {
				TenantPersistenceService.getInstance().beginTransaction();
				SiteController siteController = SiteController.staticGetDao().findByDomainId(null,
					getCurrentUserContext().getUsername());
				// This was klunky, following the model for CHE network refresh.
				// But putwall update is for the facility and not just one siteController.
				if (siteController != null) {
					Facility facility = siteController.getParent().getParent();
					workService.reinitPutWallFeedback(facility);
					TenantPersistenceService.getInstance().commitTransaction();
					long totalMillis = System.currentTimeMillis() - startMillis;
					LOGGER.info("{}ms for putwall update", totalMillis);
				}
				// schedule next putwall update
				this.setNextPutWallRefresh(System.currentTimeMillis() + putwallUpdateInterval);
			} catch (Exception e) {
				LOGGER.error("Failed to send PutWall update to site controller " + this.getCurrentUserContext().getUsername(), e);
				this.setNextPutWallRefresh(System.currentTimeMillis() + updateFailureDelay);
			} finally {
				TenantPersistenceService.getInstance().rollbackAnyActiveTransactions();
			}
		}
	}
}
