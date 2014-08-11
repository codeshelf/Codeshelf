package com.gadgetworks.codeshelf.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectChangeResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

// TODO: get rid of references to constants defined in IWsReqCmd

public class Listener implements ObjectEventListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);

	@Getter @Setter
	String id;
	
	@Getter
	Class<IDomainObject> persistenceClass;
	
	@Getter @Setter
	List<UUID> matchList;
		
	@Getter @Setter
	List<String> propertyNames;
	
	public Listener(Class<IDomainObject> classObject) {
		this.persistenceClass = classObject;
	}

	@Override
	public ResponseABC processObjectAdd(IDomainObject inDomainObject) {
		return this.processEvent(inDomainObject, EventType.Create);
	}

	@Override
	public ResponseABC processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		// first see if any changed properties match the properties
		boolean matchedChangedProperty = false;
		for (String propertyName : this.propertyNames) {
			if ((inChangedProperties != null) && (inChangedProperties.contains(propertyName))) {
				matchedChangedProperty = true;
				break;
			}
		}
		if (matchedChangedProperty) {
			return this.processEvent(inDomainObject, EventType.Update);
		}
		return null;
	}

	@Override
	public ResponseABC processObjectDelete(IDomainObject inDomainObject) {
		return this.processEvent(inDomainObject, EventType.Delete);
	}
	
	private ResponseABC processEvent(IDomainObject inDomainObject, EventType type) {
		List<IDomainObject> domainObjectList = new ArrayList<IDomainObject>();
		if (this.matchList.contains(inDomainObject.getPersistentId())) {
			domainObjectList.add(inDomainObject);
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
	
	public List<Map<String, Object>> getProperties(List<IDomainObject> inDomainObjectList, EventType type) {
		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : inDomainObjectList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();
				// Always include the class name and persistent ID in the results.
				propertiesMap.put(IWsReqCmd.CLASSNAME, matchedObject.getClassName());
				propertiesMap.put(IWsReqCmd.OP_TYPE, type.toString());
				propertiesMap.put(IWsReqCmd.PERSISTENT_ID, matchedObject.getPersistentId());
				// If this is a tree object then get the parent ID as well.
				if (matchedObject instanceof IDomainObjectTree<?>) {
					IDomainObject parent = ((IDomainObjectTree<?>) matchedObject).getParent();
					if (parent != null) {
						propertiesMap.put(IWsReqCmd.PARENT_ID, parent.getPersistentId());
					}
				}
				for (String propertyName : this.propertyNames) {
					// Execute the "get" method against the parents to return the children.
					// (The method *must* start with "get" to ensure other methods don't get called.)
					String rememberGetterName = "";
					try {
						String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
						rememberGetterName = getterName;
						//String getterName = "get" + propertyName;
						java.lang.reflect.Method method = matchedObject.getClass().getMethod(getterName, (Class<?>[]) null);
						Object resultObject = method.invoke(matchedObject, (Object[]) null);
						propertiesMap.put(propertyName, resultObject);
					} catch (NoSuchMethodException e) {
						// Minor problem. UI hierarchical view asks for same data field name for all object types in the view. Not really an error in most cases
						LOGGER.debug("Method not found in ObjectListenerWsReqCmd getProperties: " + rememberGetterName);
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
}
