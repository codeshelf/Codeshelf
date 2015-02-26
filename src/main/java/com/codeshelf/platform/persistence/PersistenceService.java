package com.codeshelf.platform.persistence;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;

public abstract class PersistenceService<SCHEMA_TYPE extends Schema> extends AbstractIdleService implements IPersistenceService<SCHEMA_TYPE> {
	private static final int MAX_INITIALIZE_WAIT_SECONDS	= 60;
	static final Logger LOGGER	= LoggerFactory.getLogger(PersistenceServiceImpl.class);

	// define behavior of service
	@Override
	public abstract SCHEMA_TYPE getDefaultSchema(); // default (or single) tenant definition

	// methods for specified schema
	@Override
	public abstract Session getSession(SCHEMA_TYPE schema);
	@Override
	public abstract SessionFactory getSessionFactory(SCHEMA_TYPE schema);
	@Override
	public abstract EventListenerIntegrator getEventListenerIntegrator(SCHEMA_TYPE schema);
	@Override
	public abstract void forgetInitialActions(SCHEMA_TYPE schema);
	@Override
	public abstract boolean hasActiveTransaction(SCHEMA_TYPE schema);

	// methods that affect all sessions
	@Override
	public abstract boolean hasAnyActiveTransactions();
	@Override
	public abstract boolean rollbackAnyActiveTransactions();

	// implemented methods below
	@Override
	public String serviceName() {
		return this.getClass().getSimpleName();
	}

	protected final static Transaction beginTransaction(Session session) {
		if(session==null)
			return null;
		
		Transaction tx = session.getTransaction();
		if (tx != null) {
			// check for already active transaction
			if (tx.isActive()) {
				LOGGER.error("tried to begin transaction, but was already in active transaction");
				return tx;
			} // else we will begin new transaction
		}
		Transaction txBegun = session.beginTransaction();
		return txBegun;
	}
	
	@Override
	public final Session getSessionWithTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		beginTransaction(session);
		return session;
	}

	@Override
	public final Transaction beginTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		return beginTransaction(session);
	}
	
	@Override
	public final void commitTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		if(session != null) {
			Transaction tx = session.getTransaction();
			if (tx.isActive()) {
				tx.commit();
			} else {
				LOGGER.error("tried to close inactive Tenant transaction");
			}
		}
	}

	@Override
	public final void rollbackTransaction(SCHEMA_TYPE schema) {
		Session session = getSession(schema);
		if(session != null) {
			Transaction tx = session.getTransaction();
			if (tx.isActive()) {
				tx.rollback();
			} else {
				LOGGER.error("tried to roll back inactive Tenant transaction");
			}
		}
	}

	/* Methods for using default schema */
	@Override
	public final Transaction beginTransaction() {
		return beginTransaction(getDefaultSchema());
	}
	@Override
	public final void commitTransaction() {
		commitTransaction(getDefaultSchema());
	}
	@Override
	public final void rollbackTransaction() {
		rollbackTransaction(getDefaultSchema());
	}	
	@Override
	public final Session getSession() {
		return getSession(getDefaultSchema());
	}
	@Override
	public final Session getSessionWithTransaction() {
		return getSessionWithTransaction(getDefaultSchema());
	}
	@Override
	public final SessionFactory getSessionFactory() {
		return getSessionFactory(getDefaultSchema());
	}
	@Override
	public final EventListenerIntegrator getEventListenerIntegrator() {
		return getEventListenerIntegrator(getDefaultSchema());
	}
	void forgetInitialActions() {
		forgetInitialActions(getDefaultSchema());
	}
	boolean hasActiveTransaction() {
		return hasActiveTransaction(getDefaultSchema());
	}
	
	// service methods
	@Override
	public void awaitRunningOrThrow() {
		try {
			this.awaitRunning(MAX_INITIALIZE_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout initializing "+serviceName(),e);
		}
	}
}
