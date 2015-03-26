package com.codeshelf.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.IDomainObjectTree;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.ws.jetty.protocol.response.ObjectChangeResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class Filter implements ObjectEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(Filter.class);

	private static final int MAX_FILTER_RECORDS = 1000; 
	String	PERSISTENT_ID		= "persistentId";
	String	PARENT_ID			= "parentPersistentId";
	String	CLASSNAME			= "className";
	String	OP_TYPE				= "op";

	@Getter
	String id;
	
	@Getter
	Class<? extends IDomainObject> persistenceClass;
	
	@Getter @Setter
	List<UUID> matchList;
		
	@Getter @Setter
	List<String> propertyNames;

	
	final ITypedDao<? extends IDomainObject> dao;
	
	@Getter @Setter
	String criteriaName;
	
	@Getter @Setter
	Map<String,Object> params;

	
	PropertyUtilsBean propertyUtils = new PropertyUtilsBean();

	public Filter(ITypedDao<? extends IDomainObject> dao, Class<? extends IDomainObject> persistenceClass, String id) {
		this.persistenceClass = persistenceClass;
		this.id = id;
		this.dao = dao;
	}

	@Override
	public ResponseABC processObjectAdd(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		ResponseABC result = null;
		if(this.getPersistenceClass().isAssignableFrom(domainClass)) {
			// TODO:???
			if (!this.matchList.contains(domainPersistentId)) {
				this.matchList.add(domainPersistentId);
			}
			result = this.processEvent(domainClass, domainPersistentId, EventType.Create);
			refreshMatchList();
		} 
		return result;
	}

	@Override
	public ResponseABC processObjectUpdate(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Set<String> inChangedProperties) {
		//rough rule of thumb to catch soft addition (where active went from false to true)
		// start by ignoring if this filter is not looking for super class
		if(this.getPersistenceClass().isAssignableFrom(domainClass)) {
			// getting NPE from MAT on long line. What is null?
			Preconditions.checkNotNull(dao, "dao is null for class " + domainClass);
			Preconditions.checkNotNull(params, "params is null for class " + domainClass); // could null be ok for this?
			
			boolean matches = dao.matchesFilter(criteriaName,params,domainPersistentId);
			if (matches) {
				if (this.matchList.contains(domainPersistentId)) {
					return this.processEvent(domainClass, domainPersistentId,  EventType.Update);
				} else {
					return processObjectAdd(domainClass, domainPersistentId);
				}
			} else if (this.matchList.contains(domainPersistentId)) {
				return processObjectDelete(domainClass, domainPersistentId, null, null);
			}
		}
		return null;

	}

	@Override
	public ResponseABC processObjectDelete(Class<? extends IDomainObject> inDomainClass, final UUID inDomainPersistentId,
			Class<? extends IDomainObject> parentClass, final UUID parentPersistentId) {
		Map<String, Object> deletedObjectProperties = getPropertiesForDeleted(inDomainClass, inDomainPersistentId);
		ObjectChangeResponse deleteResponse = new ObjectChangeResponse();
		deleteResponse.setResults(ImmutableList.of(deletedObjectProperties));
		deleteResponse.setRequestId(this.id);
		this.matchList.remove(inDomainPersistentId);
		return deleteResponse;
	}
	
	private ResponseABC processEvent(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, EventType type) {
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (this.matchList.contains(domainPersistentId)) {
			IDomainObject domainObject = TenantPersistenceService.getInstance().getDao(domainClass).findByPersistentId(domainPersistentId);
			if (domainObject != null) {
				domainObjectList.add(domainObject);
			} else {
				LOGGER.warn("listener unable to find persistentId: " + domainPersistentId);
			}
		}
		if (domainObjectList.size()>0) {
			List<Map<String, Object>> p = getProperties(domainObjectList,type);
			if (p!=null) {
				ObjectChangeResponse response = new ObjectChangeResponse();
				response.setResults(p);
				response.setRequestId(this.id);
				return response;
			}
		}	
		return null;
	}	
	
	public Map<String, Object> getPropertiesForDeleted(Class<? extends IDomainObject> inDomainClass, UUID inPersistentId) {
		Map<String, Object> propertiesMap = Maps.newHashMap();
		// Always include the class name and persistent ID in the results.
		propertiesMap.put(CLASSNAME, inDomainClass.getSimpleName());
		propertiesMap.put(OP_TYPE, EventType.Delete.toString());
		propertiesMap.put(PERSISTENT_ID, inPersistentId);
		return propertiesMap;
		
	}
	
	public List<Map<String, Object>> getProperties(List<? extends IDomainObject> inDomainObjectList, EventType type) {
		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : inDomainObjectList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();
				// Always include the class name and persistent ID in the results.
				propertiesMap.put(CLASSNAME, matchedObject.getClassName());
				propertiesMap.put(OP_TYPE, type.toString());
				propertiesMap.put(PERSISTENT_ID, matchedObject.getPersistentId());
				// If this is a tree object then get the parent ID as well.
				if (matchedObject instanceof IDomainObjectTree<?>) {
					IDomainObject parent = ((IDomainObjectTree<?>) matchedObject).getParent();
					if (parent != null) {
						propertiesMap.put(PARENT_ID, parent.getPersistentId());
					}
				}
				for (String propertyName : this.propertyNames) {
					try {
						Object resultObject = propertyUtils.getProperty(matchedObject, propertyName);
						propertiesMap.put(propertyName, resultObject);
					} catch(NoSuchMethodException e) {
						// Minor problem. UI hierarchical view asks for same data field name for all object types in the view. Not really an error in most cases
						LOGGER.debug("no property " +propertyName + " on object: " + matchedObject);
					} catch(Exception e) {
						LOGGER.warn("unexpected exception for property " +propertyName + " object: " + matchedObject, e);
					}
				}
				resultsList.add(propertiesMap);
			}
			if (resultsList.size() > 0) {
				return resultsList;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to get Listener properties", e);
		}			
		return null;
	}			

	public List<? extends IDomainObject> refreshMatchList() {
		List<? extends IDomainObject> objectMatchList = dao.findByFilter(criteriaName,params, MAX_FILTER_RECORDS);
		List<UUID> objectIds = new LinkedList<UUID>();
		for (IDomainObject object : objectMatchList) {
			objectIds.add(object.getPersistentId());
		}
		this.setMatchList(objectIds);
		return objectMatchList;
	}

}
