package com.codeshelf.platform.persistence;

import java.util.UUID;

import org.hibernate.Hibernate;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.DomainObjectTreeABC;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.IDomainObjectTree;

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
		if (DomainObjectABC.class.isAssignableFrom(entity.getClass())) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;

			Class<? extends IDomainObject> parentClass = null;
			UUID parentId = null;
			if(DomainObjectTreeABC.class.isAssignableFrom(entity.getClass())) {
				IDomainObjectTree<? extends IDomainObject> childObject = (IDomainObjectTree<? extends IDomainObject>) entity;
				IDomainObject parent = childObject.getParent();
				if(parent != null) {
					parentClass = parent.getClass();
					parentId = childObject.getParentPersistentId();
				}
			}
			this.objectChangeBroadcaster.broadcastDelete(Hibernate.getClass(domainObject), domainObject.getPersistentId(),
				parentClass, parentId);
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
