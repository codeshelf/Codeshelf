/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: MockDao.java,v 1.5 2012/10/29 02:59:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;

/**
 * @author jeffw
 *
 */
public class MockDao<T extends IDomainObject> implements ITypedDao<T> {

	private Map<String, T>	mStorage	= new HashMap<String, T>();

	public MockDao() {

	}

	public void registerDAOListener(IDaoListener inListener) {

	}

	public void unregisterDAOListener(IDaoListener inListener) {

	}

	public void removeDAOListeners() {

	}

	public final Query<T> query() {
		// TODO Auto-generated method stub
		return null;
	}

	public final T findByPersistentId(Long inPersistentId) {
		// TODO Auto-generated method stub
		return null;
	}

	public final T findByDomainId(IDomainObject inParentObject, String inDomainId) {
		String domainId = "";
		if ((inParentObject != null)) {
			domainId = inParentObject.getFullDomainId() + "." + inDomainId;
		} else {
			domainId = inDomainId;
		}
		return mStorage.get(domainId);
	}

	public final List<T> findByPersistentIdList(List<Long> inPersistentIdList) {
		// TODO Auto-generated method stub
		return null;
	}

	public final List<T> findByFilter(String inFilter, Map<String, Object> inFilterParams) {
		// TODO Auto-generated method stub
		return null;
	}

	public void store(T inDomainObject) throws DaoException {
		mStorage.put(inDomainObject.getFullDomainId(), inDomainObject);
		inDomainObject.setPersistentId((long) inDomainObject.getFullDomainId().hashCode());
	}

	public void delete(T inDomainObject) throws DaoException {
		mStorage.remove(inDomainObject).getFullDomainId();
	}

	public List<T> getAll() {
		return new ArrayList(mStorage.values());
	}

	public void pushNonPersistentUpdates(T inDomainObject) {
	}

	public Class<T> getDaoClass() {
		return null;
	}

	@Override
	public <L> List<L> findByFilterAndClass(String inFilter, Map<String, Object> inFilterParams, Class<L> inClass) {
		// TODO Auto-generated method stub
		return null;
	}
}
