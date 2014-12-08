package com.gadgetworks.codeshelf.platform.persistence;

import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;

public class InsertBroadcastListener implements PostCommitInsertEventListener {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DeleteBroadcastListener.class);

	private ObjectChangeBroadcaster	objectChangeBroadcaster;
	
	public InsertBroadcastListener(ObjectChangeBroadcaster objectChangeBroadcaster) {
		super();
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}
	@Override
	public void onPostInsert(PostInsertEvent event) {
		Object entity = event.getEntity();
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			this.objectChangeBroadcaster.broadcastAdd(domainObject.getClass(), domainObject.getPersistentId());
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
