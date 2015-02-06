package com.gadgetworks.codeshelf.platform.persistence;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.google.common.base.Objects;

public class UpdateBroadcastListener implements PostCommitUpdateEventListener {

	private static final long	serialVersionUID	= 1L;

	private static final Logger	LOGGER	= LoggerFactory.getLogger(UpdateBroadcastListener.class);

	private ObjectChangeBroadcaster	objectChangeBroadcaster;
	
	public UpdateBroadcastListener(ObjectChangeBroadcaster objectChangeBroadcaster) {
		super();
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		Object entity = event.getEntity();
		Object[] previousState = event.getOldState();
		Object[] currentState = event.getState();
		String[] allPropertyNames = event.getPersister().getPropertyNames();
		int[] dirtyPropertyIndexes = event.getDirtyProperties();
	
		if (entity instanceof DomainObjectABC) {
			// update domain object
			Set<String> changedProperties = new HashSet<String>();
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			if(previousState != null) {
				for (int dirtyIndex : dirtyPropertyIndexes) {
					String propertyName = allPropertyNames[dirtyIndex];
					Object previous = previousState[dirtyIndex];
					Object current = currentState[dirtyIndex];
					if (!Objects.equal(previous, current)){
						LOGGER.trace(propertyName +" changed from "+previous+" to "+current);
						changedProperties.add(propertyName);
					}
				}
				if (changedProperties.size()>0) {
					@SuppressWarnings("unchecked")
					Class<? extends IDomainObject> clazz = Hibernate.getClass(domainObject);
					this.objectChangeBroadcaster.broadcastUpdate(clazz, domainObject.getPersistentId(), changedProperties);
				}
			}
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void onPostUpdateCommitFailed(PostUpdateEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}
