package com.codeshelf.persistence;

import org.hibernate.Session;
import org.hibernate.Transaction;

public abstract class SideTransaction<V> {
	
	public abstract V task(Session session);
	
	public V run() throws Exception{
		Session session = null;
		Transaction transaction = null;
		V result = null;
		try {
			session = TenantPersistenceService.getInstance().openNewSession();
			transaction = session.beginTransaction();
			result = task(session);
		} catch (Exception e) {
			rollback(session, transaction);
			throw e;
		}
		commit(session, transaction);
		return result;
	}
	
	private void rollback(Session session, Transaction transaction){
		if (transaction != null && transaction.isActive()) {
			transaction.rollback();
		}
		closeSession(session);
		
	}
	
	private void commit(Session session, Transaction transaction){
		if (transaction != null && transaction.isActive()) {
			transaction.commit();
		}
		closeSession(session);
	}
	
	private void closeSession(Session session){
		if (session != null && session.isOpen()) {
			session.close();
		}
	}
}