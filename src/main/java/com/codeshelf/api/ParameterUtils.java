package com.codeshelf.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

public class ParameterUtils {
	
	public static Map<String, String> toMapOfFirstValues(MultivaluedMap<String, String> functionParams) {
		HashMap<String, String> firstValues = new HashMap<>();
		for (Entry<String, List<String>> entry : functionParams.entrySet()) {
			String value = functionParams.getFirst(entry.getKey());
			firstValues.put(entry.getKey(), value);
		}
		return firstValues;
	}
}
