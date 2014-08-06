package com.gadgetworks.codeshelf.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.RegisterFilterResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

import lombok.Getter;
import lombok.Setter;

public class Filter implements IObjectEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(Filter.class);

	@Getter @Setter
	List<String> propertyNames;
	
	@Getter
	Class<IDomainObject> persistenceClass;
	
	// private List<IDomainObject> objectMatchList;
	
	@Setter
	private String filterClause;
	
	@Setter
	private Map<String, Object> filterParams;
	
	@Setter
	ITypedDao<IDomainObject> dao;
	
	@Getter @Setter
	String id;
	
	public Filter(Class<IDomainObject> clazz) {
		this.persistenceClass = clazz;
	}

	public List<Map<String, Object>> getProperties(String inOperationType) {
		try {
			List<IDomainObject> objectMatchList = dao.findByFilter(this.filterClause, this.filterParams);
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : objectMatchList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();
				// Always include the class name and persistent ID in the results.
				propertiesMap.put(IWsReqCmd.CLASSNAME, matchedObject.getClassName());
				propertiesMap.put(IWsReqCmd.OP_TYPE, inOperationType);
				propertiesMap.put(IWsReqCmd.PERSISTENT_ID, matchedObject.getPersistentId());
				// If this is a tree object then get the parent ID as well.
				if (matchedObject instanceof IDomainObjectTree<?>) {
					propertiesMap.put(IWsReqCmd.PARENT_ID, ((IDomainObjectTree<?>) matchedObject).getParent().getPersistentId());
				}
				for (String propertyName : propertyNames) {
					// Execute the "get" method against the parents to return the children.
					// (The method *must* start with "get" to ensure other methods don't get called.)
					// Capitalize the property name to invoke the getter for it.
					String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
					String rememberGetterName = getterName;

					try {
						java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
						Class<?> methodReturnType = method.getReturnType();
						Object resultObject = method.invoke(matchedObject, (Object[]) null);
						propertiesMap.put(propertyName, resultObject);
					} catch (NoSuchMethodException e) {
						// Minor problem. UI hierarchical view asks for same data field name for all object types in the view. Not really an error in most cases
						LOGGER.debug("Method " + rememberGetterName+" not found in class "+matchedObject.getClassName());
					}
				}
				resultsList.add(propertiesMap);
			}

			if (resultsList.size() > 0) {
				return resultsList;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to get Filter properties", e);
		}
		return null;
	}

	@Override
	public ResponseABC processObjectAdd(IDomainObject inDomainObject) {
		// check if classes match
		if (!inDomainObject.getClass().equals(this.persistenceClass)) {
			return null;
		}		
		ITypedDao<IDomainObject> dao = inDomainObject.getDao();
		List<IDomainObject> objectMatchList = dao.findByFilter(filterClause, filterParams);
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (objectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);			
			List<Map<String, Object>> p = getProperties(IWsReqCmd.OP_TYPE_CREATE);
			if (p!=null) {
				RegisterFilterResponse response = new RegisterFilterResponse();
				response.setResults(p);
				response.setRequestId(this.id);
				return response;
			}
		}
		return null;
	}

	@Override
	public ResponseABC processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		// check if classes match
		if (!inDomainObject.getClass().equals(this.persistenceClass)) {
			return null;
		}		
		// first see if any changed properties match the filtered properties.
		boolean matchedChangedProperty = false;
		for (String propertyName : this.propertyNames) {
			if ((inChangedProperties != null) && (inChangedProperties.contains(propertyName))) {
				matchedChangedProperty = true;
				break;
			}
		}
		// send update if property matches
		if (matchedChangedProperty) {
			List<IDomainObject> objectMatchList = dao.findByFilter(this.filterClause, this.filterParams);
			objectMatchList = dao.findByFilter(filterClause, filterParams);
			List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
			domainObjectList.add(inDomainObject);
			if (objectMatchList.contains(inDomainObject)) {
				List<Map<String, Object>> p = getProperties(IWsReqCmd.OP_TYPE_UPDATE);
				if (p!=null) {
					RegisterFilterResponse response = new RegisterFilterResponse();
					response.setResults(p);
					response.setRequestId(this.id);
					return response;
				}
			} else {
				List<Map<String, Object>> p = getProperties(IWsReqCmd.OP_TYPE_DELETE);
				if (p!=null) {
					RegisterFilterResponse response = new RegisterFilterResponse();
					response.setResults(p);
					response.setRequestId(this.id);
					return response;
				}
			}
		}
		return null;
	}

	@Override
	public ResponseABC processObjectDelete(IDomainObject inDomainObject) {
		if (!inDomainObject.getClass().equals(this.persistenceClass)) {
			// different class type
			return null;
		}
		List<IDomainObject> objectMatchList = dao.findByFilter(this.filterClause, this.filterParams);
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (objectMatchList.contains(inDomainObject)) {
			domainObjectList.add(inDomainObject);
			List<Map<String, Object>> p = getProperties(IWsReqCmd.OP_TYPE_DELETE);
			if (p!=null) {
				RegisterFilterResponse response = new RegisterFilterResponse();
				response.setResults(p);
				response.setRequestId(this.id);
				return response;
			}
		}
		return null;
	}
}
