/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: MockDao.java,v 1.13 2013/04/11 20:26:44 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.testframework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;

import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.IDaoListener;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.DomainObjectTreeABC;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.IDomainObjectTree;
import com.eaio.uuid.UUIDGen;

/**
 * @author jeffw
 *
 */
public class MockDao<T extends IDomainObject> implements ITypedDao<T> {
	private Map<String, T>	storageByFullDomainId	= new HashMap<String, T>(); // e.g. "O1.user@email"
	private Map<String, T>	storageByDomainIdOnly	= new HashMap<String, T>(); // e.g. "user@email" for searching without specifying parent
	private Map<UUID, T>	storageByPersistentId	= new HashMap<UUID, T>();
	@Getter
	@Setter
	private Class<T> daoClass;

	public MockDao(Class <T> clazz) {
		setDaoClass(clazz);
	}

	public final void registerDAOListener(IDaoListener inListener) {
	}

	public final void unregisterDAOListener(IDaoListener inListener) {
	}

	public final void removeDAOListeners() {
	}

	@SuppressWarnings("rawtypes")
	private String getFullDomainId(IDomainObject inDomainObject) {
		String result = "";
		if (inDomainObject instanceof IDomainObjectTree) {
			result = ((IDomainObjectTree) inDomainObject).getFullDomainId();
		} else {
			result = inDomainObject.getDomainId();
		}
		return result;
	}

	public final T findByPersistentId(UUID inPersistentId) {
		return storageByPersistentId.get(inPersistentId);
	}

	public final T findByDomainId(IDomainObject inParentObject, String inDomainId) {
		String domainId = "";
		if ((inParentObject != null)) {
			domainId = getFullDomainId(inParentObject) + "." + inDomainId;
			return storageByFullDomainId.get(domainId);
		} //else 
		return storageByDomainIdOnly.get(inDomainId);
	}

	public final List<T> findByPersistentIdList(List<UUID> inPersistentIdList) {
		throw new NotImplementedException();
	}
	
	public final List<T> findByParentPersistentIdList(List<UUID> inIdList){
		throw new NotImplementedException();
	}

	public final List<T> findByFilter(List<Criterion> inFilter) {
		throw new NotImplementedException();
	}

	@Override
	public List<T> findByFilter(List<Criterion> inFilter, List<Order> inOrderBys) {
		throw new NotImplementedException();
	}

	
	public final void store(IDomainObject inDomainObject) {
		validateClass(inDomainObject.getClass());
		@SuppressWarnings("unchecked")
		T inTypedObject = (T) inDomainObject;
		
		inTypedObject.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		inTypedObject.setVersion(System.currentTimeMillis());

		// Walk through all of the fields to see if any of them are null but should not be.
		try {
			Collection<Field> fields = new ArrayList<Field>();
			addDeclaredAndInheritedFields(inTypedObject.getClass(), fields);
			for (Field field : fields) {
				for (Annotation annotation : field.getAnnotations()) {
					if (annotation.annotationType().equals(Column.class)) {
						Column column = (Column) annotation;
						field.setAccessible(true);
						if ((!column.nullable()) && (field.get(inTypedObject) == null)) {
							throw new NullPointerException("Field: " + field.getName() + " is null and shouldn't be according to @Column.");
						}
					}
				}
			}
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			System.err.println("");
			e.printStackTrace();
		}

		storageByFullDomainId.put(getFullDomainId(inTypedObject), inTypedObject);
		storageByDomainIdOnly.put(inTypedObject.getDomainId(), inTypedObject);
		storageByPersistentId.put(inTypedObject.getPersistentId(), inTypedObject);
	}

	private static void addDeclaredAndInheritedFields(Class<?> c, Collection<Field> fields) {
	    fields.addAll(Arrays.asList(c.getDeclaredFields())); 
	    Class<?> superClass = c.getSuperclass(); 
	    if (superClass != null) { 
	        addDeclaredAndInheritedFields(superClass, fields); 
	    }       
	}
	
	public final void delete(IDomainObject inDomainObject) {
		validateClass(inDomainObject.getClass());
		
		storageByDomainIdOnly.remove(inDomainObject.getDomainId());
		storageByFullDomainId.remove(getFullDomainId(inDomainObject));
		storageByPersistentId.remove(inDomainObject.getPersistentId());
	}

	private void validateClass(Class<? extends IDomainObject> clazz) {
		if(!this.getDaoClass().isAssignableFrom(clazz)) {
			throw new DaoException("expected type "+this.getDaoClass().getSimpleName()+ " but got "+clazz.getSimpleName());
		}
	}

	public final List<T> getAll() {
		return new ArrayList<T>(storageByDomainIdOnly.values());
	}

	public final void pushNonPersistentUpdates(T inDomainObject) {
	}

	@Override
	public final List<T> findByFilter(String criteria, Map<String, Object> inFilterParams) {
		throw new NotImplementedException();
	}

	@Override
	public T findByPersistentId(String inPersistentIdAsString) {
		UUID persistentId = UUID.fromString(inPersistentIdAsString);
		return this.findByPersistentId(persistentId);
	}

	@Override
	public boolean matchesFilter(String criteriaName, Map<String, Object> inFilterArgs, UUID persistentId) {
		throw new NotImplementedException();
	}

	@Override
	public T reload(T domainObject) {
		return domainObject;
		//throw new NotImplementedException();
	}

	@Override
	public List<T> findByCriteriaQuery(Criteria criteria) {
		throw new NotImplementedException();
	}
	
	@Override
	public int countByCriteriaQuery(Criteria criteria)
	{
		throw new NotImplementedException();
	}

	@Override
	public List<UUID> getUUIDListByCriteriaQuery(Criteria criteria) {
		throw new NotImplementedException();
	}

	@Override
	public Criteria createCriteria() {
		throw new NotImplementedException();
	}

	@Override
	public List<T> findByParent(IDomainObject inParentObject) {
		//throw new NotImplementedException();
		List<T> result = new LinkedList<T>();
		for (T e : this.storageByPersistentId.values()) {
			@SuppressWarnings("unchecked")
			DomainObjectTreeABC<T> dom = (DomainObjectTreeABC<T>) e;
			if (dom.getParent().equals(inParentObject)) {
				result.add(e);
			}
		}
		return result;
	}

}
