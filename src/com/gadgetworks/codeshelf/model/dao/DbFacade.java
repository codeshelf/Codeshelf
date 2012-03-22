/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DbFacade.java,v 1.2 2012/03/22 07:35:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;
import java.util.List;

import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class DbFacade<T extends PersistABC> implements IDbFacade<T> {

	private static final Log	LOGGER	= LogFactory.getLog(DbFacade.class);

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDbFacade#findByPersistentId(java.lang.Class, long)
	 */
	public final T findByPersistentId(Class<T> inClass, long inPersistentId) {
		T result = null;
		try {
			result = Ebean.find(inClass, inPersistentId);
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDbFacade#findByDomainId(java.lang.Class, java.lang.String)
	 */
	public final T findByDomainId(Class<T> inClass, String inId) {
		T result = null;
		try {
			Query<T> query = Ebean.createQuery(inClass);
			query.where().eq(T.getIdColumnName(), inId);
			//query = query.setUseCache(true);
			result = query.findUnique();
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDbFacade#findByIdList(java.util.List)
	 */
	public final List<T> findByPersistentIdList(Class<T> inClass, List<Long> inIdList) {
		Query<T> query = Ebean.find(inClass);
		List<T> methodResultsList = query.where().in("persistentId", inIdList).findList();
		return methodResultsList;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDbFacade#getAll()
	 */
	public final Collection<T> getAll(final Class<T> inClass) {
		Query<T> query = Ebean.createQuery(inClass);
		query = query.setUseCache(true);
		return query.findList();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDbFacade#save(com.gadgetworks.codeshelf.model.persist.PersistABC)
	 */
	public final void save(T inDomainObject) {
		Ebean.save(inDomainObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDbFacade#delete(com.gadgetworks.codeshelf.model.persist.PersistABC)
	 */
	public final void delete(T inDomainObject) {
		Ebean.delete(inDomainObject);
	}
}
