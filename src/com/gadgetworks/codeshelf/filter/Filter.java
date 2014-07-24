package com.gadgetworks.codeshelf.filter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;

import lombok.Getter;
import lombok.Setter;

public class Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(Filter.class);

	@Getter @Setter
	List<String> propertyNames;

	public List<Map<String, Object>> getProperties(List<IDomainObject> inDomainObjectList, String inOperationType) {

		try {

			List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
			for (IDomainObject matchedObject : inDomainObjectList) {
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
						LOGGER.debug("Method not found in ObjectFilterWsReqCmd getProperties: " + rememberGetterName);
					}
				}
				resultsList.add(propertiesMap);
			}

			if (resultsList.size() > 0) {
				// Convert the list of objects into a JSon object.
				/*
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode dataNode = mapper.createObjectNode();
				ArrayNode searchListNode = mapper.valueToTree(resultsList);
				dataNode.put(RESULTS, searchListNode);

				result = new ObjectFilterWsRespCmd(dataNode);
				*/
				return resultsList;
			}
		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		}
		return null;
	}

}
