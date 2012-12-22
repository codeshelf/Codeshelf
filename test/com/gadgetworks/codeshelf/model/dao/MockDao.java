/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: MockDao.java,v 1.8 2012/12/22 09:36:38 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;

/**
 * @author jeffw
 *
 */
public class MockDao<T extends IDomainObject> implements ITypedDao<T> {

	private Map<String, T>	mStorage	= new HashMap<String, T>();

	public MockDao() {

	}

	public final void registerDAOListener(IDaoListener inListener) {

	}

	public final void unregisterDAOListener(IDaoListener inListener) {

	}

	public final void removeDAOListeners() {

	}
	
	private String getFullDomainId(IDomainObject inDomainObject) {
		String result = "";
		
		if (inDomainObject instanceof IDomainObjectTree ) {
			result = ((IDomainObjectTree) inDomainObject).getFullDomainId();
		} else {
			result = inDomainObject.getDomainId();
		}
		
		return result;
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
			domainId = getFullDomainId(inParentObject) + "." + inDomainId;
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

	public final void store(T inDomainObject) {
		mStorage.put(getFullDomainId(inDomainObject), inDomainObject);
		inDomainObject.setPersistentId((long) getFullDomainId(inDomainObject).hashCode());
	}

	public final void delete(T inDomainObject) {
		mStorage.remove(inDomainObject);
	}

	public final List<T> getAll() {
		return new ArrayList(mStorage.values());
	}

	public final void pushNonPersistentUpdates(T inDomainObject) {
	}

	public final Class<T> getDaoClass() {
		return null;
	}

	@Override
	public final <L> List<L> findByFilterAndClass(String inFilter, Map<String, Object> inFilterParams, Class<L> inClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getNextId(Class<?> beanType) {
		// TODO Auto-generated method stub
		return null;
	}
}
