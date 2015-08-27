package com.codeshelf.model.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.Criteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.ItemMaster;

public class DomainObjectCache<T extends DomainObjectABC> {

	private static final Logger	LOGGER			= LoggerFactory.getLogger(DomainObjectCache.class);

	@Getter
	@Setter
	int							maxPrefetchSize	= 10000;

	@Getter
	@Setter
	boolean						fetchOnMiss		= true;

	Map<String, T>				objectCache		= new HashMap<String, T>();

	final ITypedDao<T>			dao;

	IDomainObject				parent			= null;

	@Getter
	String						cacheName		= null;

	public DomainObjectCache(ITypedDao<T> dao, String inCacheName) {
		this.dao = dao;
		this.cacheName = inCacheName;
	}

	/**
	 * Calls through to its Map
	 */
	public boolean containsKey(String inKey) {
		if (objectCache == null)
			return false;
		else
			return objectCache.containsKey(inKey);
	}

	public void loadAll() {
		Criteria crit = this.dao.createCriteria();
		if (this.dao.getDaoClass().equals(ItemMaster.class)) {
			crit.addOrder(Property.forName("updated").desc());
		}
		crit.setMaxResults(maxPrefetchSize);
		List<T> list = dao.findByCriteriaQuery(crit);
		for (T item : list) {
			objectCache.put(item.getDomainId(), item);
		}
	}

	public void load(IDomainObject parent) {
		load(parent, null);
	}

	public void load(IDomainObject parent, Set<String> domainIds) {
		if (domainIds == null || domainIds.size() == 0) {
			// nothing to load
			return;
		}
		this.parent = parent;
		UUID persistentId = parent.getPersistentId();
		Criteria crit = this.dao.createCriteria();
		crit.add(Restrictions.eq("parent.persistentId", persistentId));
		crit.add(Restrictions.in("domainId", domainIds));
		// little bit of a hack: sort by update time stamp for item
		// master to implement LRU prefetching.  would be better to have 
		// update time stamp on all domain objects.
		if (this.dao.getDaoClass().equals(ItemMaster.class)) {
			crit.addOrder(Property.forName("updated").desc());
		}
		crit.setMaxResults(maxPrefetchSize);
		List<T> list = dao.findByCriteriaQuery(crit);
		for (T item : list) {
			objectCache.put(item.getDomainId(), item);
		}
		// Looking for possible object cache bugs. If absolutely nothing, it might be reasonable--only new domainIds in the file.
		if (objectCache.size() == 0) {
			LOGGER.warn("{} cache: None loaded from database.", this.getCacheName());
		}
	}

	public T get(String domainId) {
		T obj = this.objectCache.get(domainId);
		if (obj == null && this.fetchOnMiss) {
			// try to retrieve from database on miss
			obj = this.dao.findByDomainId(this.parent, domainId);
			if (obj != null) {
				// add to cache
				objectCache.put(obj.getDomainId(), obj);
				if (objectCache.size() < maxPrefetchSize)
					LOGGER.error("Bad {} cache maintenance? {} found in database and now added to cache", domainId, cacheName);
				// If we preloaded to the maxPrefetchSize, then new ones can still be added. This would be a massive file situation as only
				// IDs found in the file are preloaded.
			}
		}
		return obj;
	}

	public void put(T obj) {
		objectCache.put(obj.getDomainId(), obj);
	}

	public void remove(T obj) {
		objectCache.remove(obj.getDomainId());
	}

	public int size() {
		return this.objectCache.size();
	}

	public void reset() {
		this.objectCache.clear();
	}

	public Collection<T> getAll() {
		return this.objectCache.values();
	}

}
