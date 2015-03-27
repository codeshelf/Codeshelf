package com.codeshelf.platform.persistence;

import org.hibernate.Hibernate;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.IDomainObject;

@SuppressWarnings("serial")
public class InsertBroadcastListener implements PostCommitInsertEventListener {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DeleteBroadcastListener.class);

	private ObjectChangeBroadcaster	objectChangeBroadcaster;
	
	public InsertBroadcastListener(ObjectChangeBroadcaster objectChangeBroadcaster) {
		super();
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}
	@Override
	public void onPostInsert(PostInsertEvent event) {
		Object entity = event.getEntity();
		String tenantIdentifier = event.getSession().getTenantIdentifier();
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			@SuppressWarnings("unchecked")
			Class<? extends IDomainObject> clazz = Hibernate.getClass(domainObject);
			this.objectChangeBroadcaster.broadcastAdd(tenantIdentifier,clazz, domainObject.getPersistentId());
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister arg0) {
		return true;
	}
	
	@Override
	public void onPostInsertCommitFailed(PostInsertEvent event) {
		// TODO Auto-generated method stub
		
	}

}
