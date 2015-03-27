package com.codeshelf.model.dao;

import java.util.Set;

import com.codeshelf.model.domain.IDomainObject;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class TenantListenerSet {
	private SetMultimap<Class<? extends IDomainObject>, IDaoListener> listeners;
	
	public TenantListenerSet() {
		listeners = HashMultimap.create();
	}
	
	Set<IDaoListener> get(Class<? extends IDomainObject> clazz) {
		return listeners.get(clazz);
	}

	public Set<Class<? extends IDomainObject>> keySet() {
		return listeners.keySet();
	}

	public void put(Class<? extends IDomainObject> daoClass, IDaoListener listener) {
		listeners.put(daoClass, listener);		
	}

	public void putAll(Class<? extends IDomainObject> key, Set<IDaoListener> set) {
		listeners.putAll(key,set);
	}

}
