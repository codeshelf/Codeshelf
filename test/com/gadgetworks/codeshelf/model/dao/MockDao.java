/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: MockDao.java,v 1.13 2013/04/11 20:26:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;

import org.apache.commons.lang.NotImplementedException;

import com.eaio.uuid.UUIDGen;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;

/**
 * @author jeffw
 *
 */
public class MockDao<T extends IDomainObject> implements ITypedDao<T> {

	private Map<String, T>	storageByDomainId	= new HashMap<String, T>();
	private Map<UUID, T>	storageByPersistentId	= new HashMap<UUID, T>();

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

		if (inDomainObject instanceof IDomainObjectTree) {
			result = ((IDomainObjectTree) inDomainObject).getFullDomainId();
		} else {
			result = inDomainObject.getDomainId();
		}

		return result;
	}

	public final Query<T> query() {
		throw new NotImplementedException();
	}

	public final T findByPersistentId(UUID inPersistentId) {
		return storageByPersistentId.get(inPersistentId);
	}

	@Override
	public <P extends IDomainObject> P findByPersistentId(Class<P> inClass, UUID inPersistentId) {
		return (P) storageByDomainId.get(inPersistentId);
	}

	public final T findByDomainId(IDomainObject inParentObject, String inDomainId) {
		String domainId = "";
		if ((inParentObject != null)) {
			domainId = getFullDomainId(inParentObject) + "." + inDomainId;
		} else {
			domainId = inDomainId;
		}
		return storageByDomainId.get(domainId);
	}

	public final List<T> findByPersistentIdList(List<UUID> inPersistentIdList) {
		throw new NotImplementedException();

	}

	public final List<T> findByFilter(String inFilter, Map<String, Object> inFilterParams) {
		throw new NotImplementedException();
	}

	public final void store(T inDomainObject) {
		inDomainObject.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		inDomainObject.setVersion(new Timestamp(System.currentTimeMillis()));

		// Walk through all of the fields to see if any of them are null but should not be.
		try {
			Collection<Field> fields = new ArrayList<Field>();
			addDeclaredAndInheritedFields(inDomainObject.getClass(), fields);
			for (Field field : fields) {
				for (Annotation annotation : field.getAnnotations()) {
					if (annotation.annotationType().equals(Column.class)) {
						Column column = (Column) annotation;
						field.setAccessible(true);
						if ((!column.nullable()) && (field.get(inDomainObject) == null)) {
							throw new NullPointerException("Field: " + field.getName() + " is null and shouldn't be according to @Column.");
						}
					}
				}
			}
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			System.err.println("");
			e.printStackTrace();
		}

		storageByDomainId.put(getFullDomainId(inDomainObject), inDomainObject);
		storageByPersistentId.put(inDomainObject.getPersistentId(), inDomainObject);
	}

	private static void addDeclaredAndInheritedFields(Class<?> c, Collection<Field> fields) {
	    fields.addAll(Arrays.asList(c.getDeclaredFields())); 
	    Class<?> superClass = c.getSuperclass(); 
	    if (superClass != null) { 
	        addDeclaredAndInheritedFields(superClass, fields); 
	    }       
	}
	
	public final void delete(T inDomainObject) {
		storageByDomainId.remove(inDomainObject);
		storageByPersistentId.remove(inDomainObject.getPersistentId());
	}

	public final List<T> getAll() {
		return new ArrayList(storageByDomainId.values());
	}

	public final void pushNonPersistentUpdates(T inDomainObject) {
	}

	public final Class<T> getDaoClass() {
		return null;
	}

	@Override
	public final <L> List<L> findByFilterAndClass(String inFilter, Map<String, Object> inFilterParams, Class<L> inClass) {
		throw new NotImplementedException();
	}

	@Override
	public Object getNextId(Class<?> beanType) {
		throw new NotImplementedException();
	}

	@Override
	public void beginTransaction() {
	}

	@Override
	public void endTransaction() {
	}

	@Override
	public void commitTransaction() {	
	}

	@Override
	public Boolean isNewOrDirty(IDomainObject inDomainObject) {
		throw new NotImplementedException();
	}

	@Override
	public void clearAllCaches() {
	}

	@Override
	public T findByPersistentId(String inPersistentIdAsString) {
		UUID persistentId = UUID.fromString(inPersistentIdAsString);
		return this.findByPersistentId(persistentId);
	}
}
