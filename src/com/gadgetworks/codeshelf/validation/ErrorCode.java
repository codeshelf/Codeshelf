package com.gadgetworks.codeshelf.validation;


public enum ErrorCode {
	GENERAL("error.general"),
	FIELD_REQUIRED("error.field.required"),
	FIELD_NUMBER_NOT_NEGATIVE("error.field.number.notnegative"),
	FIELD_NUMBER_REQUIRED("error.field.number.required"),
	FIELD_GENERAL("error.field.general"),
	FIELD_REFERENCE_NOT_FOUND("error.field.reference.notfound"), //value was used for a lookup and could not find the reference
	FIELD_NUMBER_BELOW_MIN("error.field.number.min"),
	FIELD_WRONG_TYPE ("error.field.conversion"),
	FIELD_REFERENCE_INACTIVE ("error.field.reference.inactive"); //value was found but was inactive
	
	private final String delimitedName;
	
	private ErrorCode(String delimitedName) {
		this.delimitedName = delimitedName;
	}

	public String toString() {
		return delimitedName;
	}
}
