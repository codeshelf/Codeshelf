package com.gadgetworks.codeshelf.platform.persistence;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;

public class HibernateInterceptor extends EmptyInterceptor {

	private static final long	serialVersionUID	= 4880090344795038441L;

	private static final Logger	LOGGER	= LoggerFactory.getLogger(HibernateInterceptor.class);

	@Override
	public boolean onFlushDirty(Object entity,
		Serializable id,
		Object[] currentState,
		Object[] previousState,
		String[] propertyNames,
		Type[] types) {
		if (entity instanceof DomainObjectABC) {
			// update domain object
			Set<String> changedProperties = new HashSet<String>();
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			for (int i=0;i<propertyNames.length;i++) {
				if (!currentState[i].equals(previousState[i])) {
					LOGGER.trace(propertyNames[i]+" changed from "+previousState[i]+" to "+currentState[i]);
					changedProperties.add(propertyNames[i]);
				}
			}
			if (changedProperties.size()>0) {
				ITypedDao<IDomainObject> dao = domainObject.getDao();
				if (dao!=null) {
					dao.broadcastUpdate(domainObject, changedProperties);
				}
				else {
					LOGGER.error("Failed to broadcast update notification. DAO for type "+domainObject.getClass().getSimpleName()+" is undefined");
				}
			}
		}
		return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
	}
	
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			ITypedDao<IDomainObject> dao = domainObject.getDao();
			if (dao!=null) {
				dao.broadcastDelete(domainObject);
			}
			else {
				LOGGER.error("Failed to broadcast delete notification. DAO for type "+domainObject.getClass().getSimpleName()+" is undefined");
			}
		}
		super.onDelete(entity, id, state, propertyNames, types);
	}
	
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof DomainObjectABC) {
			DomainObjectABC domainObject = (DomainObjectABC) entity;
			ITypedDao<IDomainObject> dao = domainObject.getDao();
			if (dao!=null) {
				dao.broadcastAdd(domainObject);
			}
			else {
				LOGGER.error("Failed to broadcast add notification. DAO for type "+domainObject.getClass().getSimpleName()+" is undefined");
			}
		}
		return super.onSave(entity, id, state, propertyNames, types);
	}
}
