package com.codeshelf.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(DomainObjectCache.class);
	
	@Getter @Setter
	int maxPrefetchSize = 10000;

	Map<String,T> objectCache =  new HashMap<String,T>();
	
	final ITypedDao<T> dao;
	
	IDomainObject parent = null;
	
	public DomainObjectCache(ITypedDao<T> dao) {		
		this.dao = dao;
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
		this.parent = parent;
		UUID persistentId = parent.getPersistentId();
		Criteria crit = this.dao.createCriteria();
		crit.add(Restrictions.eq("parent.persistentId", persistentId));
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
	}
	
	public T get(String domainId) {
		T obj = this.objectCache.get(domainId); 
		if (obj==null) {
			// try to retrieve from database on miss
			obj = this.dao.findByDomainId(this.parent, domainId);
			if (obj!=null) {
				// add to cache
				objectCache.put(obj.getDomainId(), obj);
			}
		}
		return obj;
	}

	public void put(T obj) {
		objectCache.put(obj.getDomainId(), obj);		
	}

	public int size() {
		return this.objectCache.size();
	}
	
	public void reset() {
		this.objectCache.clear();
	}

}