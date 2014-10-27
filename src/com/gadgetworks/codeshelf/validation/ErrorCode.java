package com.gadgetworks.codeshelf.validation;

public interface ErrorCode {
	
	public static final String GENERAL =  "error.general";
	public static final String	FIELD_REQUIRED	= "error.field.required";
	public static final String	FIELD_NUMBER_NOT_NEGATIVE	= "error.field.number.notnegative";
	public static final String	FIELD_NUMBER_REQUIRED	= "error.field.number.required";
	public static final String	FIELD_GENERAL	= "error.field.general";
	public static final String	FIELD_REFERENCE_NOT_FOUND	= "error.field.reference.notfound"; //value was used for a lookup and could not find the reference
	public static final String	FIELD_NUMBER_BELOW_MIN	= "error.field.number.min";
	public static final String	FIELD_WRONG_TYPE	= "error.field.conversion";
	public static final String	FIELD_REFERENCE_INACTIVE	= "error.field.reference.inactive"; //value was found but was inactive
	
}
