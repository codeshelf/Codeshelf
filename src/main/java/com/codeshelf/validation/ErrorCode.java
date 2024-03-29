package com.codeshelf.validation;

import java.text.MessageFormat;


public enum ErrorCode {
	GENERAL("error.general", ""), //expects message from caller
	FIELD_CUSTOM_MESSAGE("error.field", "{0}: {1}"), //expects message from caller
	FIELD_REQUIRED("error.field.required", "{0} is required"),
	FIELD_NUMBER_NOT_NEGATIVE("error.field.number.notnegative", "{0} with value {1} cannot be negative"),
	FIELD_NUMBER_REQUIRED("error.field.number.required", "{0} with value {1} could not be converted to a number"),
	FIELD_GENERAL("error.field.general", ""),
	FIELD_NUMBER_BELOW_MIN("error.field.number.min", "{0} with value {1} was below minimum"),
	FIELD_NUMBER_ABOVE_MAX("error.field.number.max", "{0} with value {1} was above maximum"), 
	FIELD_WRONG_TYPE ("error.field.conversion", "{0} with value {1} could not be converted"),
	FIELD_REFERENCE_NOT_FOUND("error.field.reference.notfound", "{0} with value {1} was not found"), //value was used for a lookup and could not find the reference
	FIELD_REFERENCE_NOT_UNIQUE("error.field.reference.notunique", "{0} with value {1} is not unique"),
	FIELD_REFERENCE_INACTIVE ("error.field.reference.inactive", "{0} with value {1} was not active"), //value was found but was inactive 
	FIELD_INVALID ("error.field.invalid", "{0} invalid. {1}"); // does not match approved values. May not be allowed into the database.
	
	private final String delimitedName;
	private final String defaultMessageTemplate;
	
	private ErrorCode(String delimitedName, String defaultMessageTemplate) {
		this.delimitedName = delimitedName;
		this.defaultMessageTemplate = defaultMessageTemplate;
	}

	public String toString() {
		return delimitedName;
	}
	
	public String toDefaultMessage(String field, Object erroredValue) {
		return MessageFormat.format(this.defaultMessageTemplate, field, erroredValue);
	}
}
