package com.codeshelf.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Item;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.google.common.base.Joiner;

public class FormUtility {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(FormUtility.class);
	private FormUtility() {}

	public static Map<String, String> getValidFields(MultivaluedMap<String, String> userParams, Set<String> validFields) {
		Map<String, String> result = new HashMap<String, String>();
		boolean error = false;
	
		for (String key : userParams.keySet()) {
			if (validFields.contains(key)) {
				List<String> values = userParams.get(key);
				if (values == null) {
					LOGGER.error("null value for key {}", key); // this shouldn't happen
					error = true;
					break;
				} else if (values.isEmpty()) {
					LOGGER.error("no values for key {}", key); // this shouldn't happen
					error = true;
					break;
				} else if (values.size() > 1) {
					result.put(key, Joiner.on(",").join(values)); 
					break;
				} else {
					result.put(key, values.get(0).trim()); // ok field
				}
			} else {
				LOGGER.warn("unrecognized field {}", key);
				error = true;
				break;
			}
		}
		if(result.isEmpty()) {
			LOGGER.warn("no form data");
			error = true;
		}
	
		return error ? null : result;
	};
	
	public static void throwUiValidationException(String fieldName, Object error, ErrorCode errorCode){
		DefaultErrors errors = new DefaultErrors(Item.class);
		errors.rejectValue(fieldName, error, errorCode);
		throw new InputValidationException(errors);
	}
}
