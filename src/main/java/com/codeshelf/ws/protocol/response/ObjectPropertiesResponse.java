package com.codeshelf.ws.protocol.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.IDomainObject;

public class ObjectPropertiesResponse extends ResponseABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectPropertiesResponse.class);

	@Getter @Setter
	String className;
	
	@Getter @Setter
	String persistentId;

	@Setter @Getter
	List<DomainObjectProperty> properties;
	
	@Getter @Setter
	List<Map<String, Object>> results;

	
	List<String> propertyNames = new ArrayList<String>();
	
	PropertyUtilsBean propertyUtils = new PropertyUtilsBean();

	private void setDefaultPropertyNames() {
		// Just as with filter, className must be there. Otherwise the hierarchical list view gets confused in getLevel() call.
		propertyNames.add("className");
		// The rest should come from the UI command request. Just hard code for now, matching what the UI is sending. (See domainObjects.js)
		
		propertyNames.add("persistentId");
		propertyNames.add("name");
		propertyNames.add("value");
		propertyNames.add("description");
		propertyNames.add("objectType");
		propertyNames.add("defaultValue");	
	}


	public ObjectPropertiesResponse(String className, String persistentId) {
		this.className = className;
		this.persistentId = persistentId;
		// if propertyNames parameter added to the constructor, then
		// set from that, or if null, still call
		setDefaultPropertyNames();
	}
	
	// getProperties is called on this. We want to return all appropriate fields, and not only persistent fields of each DomainObjectProperty
	
	public List<Map<String, Object>> getPropertyResults(List<DomainObjectProperty> inDomainObjectList) {
		try {
			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : inDomainObjectList) {
				Map<String, Object> propertiesMap = new HashMap<String, Object>();

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


	@Override
	public String getDeviceIdentifier() {
		return null;
	}

}
