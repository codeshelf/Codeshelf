package com.gadgetworks.codeshelf.platform.persistence;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;
import com.google.common.base.Objects;

public class UpdateBroadcastListener implements PostUpdateEventListener {

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
					this.objectChangeBroadcaster.broadcastUpdate(domainObject.getClass(), domainObject.getPersistentId(), changedProperties);
				}
			}
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister arg0) {
		return false;
	}
	
}
