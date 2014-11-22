package com.gadgetworks.codeshelf.platform.persistence;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;

public class DeleteBroadcastListener implements PostDeleteEventListener {

	private static final long	serialVersionUID	= -3661846171219447596L;

	private static final Logger	LOGGER	= LoggerFactory.getLogger(DeleteBroadcastListener.class);

	private ObjectChangeBroadcaster	objectChangeBroadcaster;
	
	public DeleteBroadcastListener(ObjectChangeBroadcaster objectChangeBroadcaster) {
		super();
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		Object entity = event.getEntity();
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			this.objectChangeBroadcaster.broadcastDelete(domainObject.getClass(), domainObject.getPersistentId());
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister arg0) {
		return false;
	}

}
