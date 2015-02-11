package com.gadgetworks.codeshelf.platform.persistence;

import org.hibernate.Hibernate;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;

public class DeleteBroadcastListener implements PostCommitDeleteEventListener {

	private static final long	serialVersionUID	= -3661846171219447596L;

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DeleteBroadcastListener.class);

	private ObjectChangeBroadcaster	objectChangeBroadcaster;
	
	public DeleteBroadcastListener(ObjectChangeBroadcaster objectChangeBroadcaster) {
		super();
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onPostDelete(PostDeleteEvent event) {
		Object entity = event.getEntity();
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			this.objectChangeBroadcaster.broadcastDelete(Hibernate.getClass(domainObject), domainObject.getPersistentId());
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister arg0) {
		return true;
	}

	@Override
	public void onPostDeleteCommitFailed(PostDeleteEvent event) {
		// TODO Auto-generated method stub
		
	}

}
