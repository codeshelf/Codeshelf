package com.gadgetworks.codeshelf.platform.persistence;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;
import com.google.inject.Inject;

public class HibernateInterceptor extends EmptyInterceptor {

	private static final long	serialVersionUID	= 4880090344795038441L;

	private static final Logger	LOGGER	= LoggerFactory.getLogger(HibernateInterceptor.class);

	private ObjectChangeBroadcaster	changeBroadcaster;

	@Inject
	public HibernateInterceptor(ObjectChangeBroadcaster changeBroadcaster) {
		this.changeBroadcaster = changeBroadcaster;
	}
	
	@Override
	public boolean onFlushDirty(Object entity,
		Serializable id,
		Object[] currentState,
		Object[] previousState,
		String[] propertyNames,
		Type[] types) {
		
		boolean result=false;
		if (entity instanceof DomainObjectABC) {
			// update domain object
			Set<String> changedProperties = new HashSet<String>();
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			result = super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);

			if(previousState != null) {
				for (int i=0;i<propertyNames.length;i++) {
					if (currentState[i]==null && previousState[i]==null) {
						continue;
					} else if (currentState[i]==null && previousState[i]!=null) {
						LOGGER.trace(propertyNames[i]+" changed from "+previousState[i]+" to "+currentState[i]);
						changedProperties.add(propertyNames[i]);
					} else if (!currentState[i].equals(previousState[i])) {
						LOGGER.trace(propertyNames[i]+" changed from "+previousState[i]+" to "+currentState[i]);
						changedProperties.add(propertyNames[i]);
					}
				}
				if (changedProperties.size()>0) {
					this.changeBroadcaster.broadcastUpdate(domainObject.getClass(), domainObject.getPersistentId(), changedProperties);
				}
			}
		}
		return result;
	}
	
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			this.changeBroadcaster.broadcastDelete(domainObject.getClass(), domainObject.getPersistentId());
		}
		super.onDelete(entity, id, state, propertyNames, types);
	}
	
	@Override
	public boolean onSave(final Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		boolean result = super.onSave(entity, id, state, propertyNames, types);
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			this.changeBroadcaster.broadcastAdd(domainObject.getClass(), domainObject.getPersistentId());
		}
		return result;

	}
}
