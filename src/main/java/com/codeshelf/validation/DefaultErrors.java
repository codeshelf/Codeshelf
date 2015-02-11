package com.codeshelf.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class DefaultErrors implements Errors, Serializable {

	private static final long	serialVersionUID	= 1L;

	@Getter
	@JsonProperty
	private List<ObjectError> globalErrors;
	
	@JsonProperty
	private Map<String, List<FieldError>> fieldErrors;

	@Getter
	private String	objectName;
	
	public DefaultErrors(Class<?> cls) {
		this.objectName = cls.getCanonicalName();
		this.globalErrors = new ArrayList<ObjectError>();
		this.fieldErrors = new HashMap<String, List<FieldError>>();
	}

	public void reject(ErrorCode errorCode, String defaultMessage) {
		this.globalErrors.add(new ObjectError(getObjectName(), new ErrorCode[]{errorCode}, new Object[]{}, defaultMessage));
	}

	public void rejectValue(String field, Object rejectedValue, ErrorCode errorCode) {
		addFieldError(field, new FieldError(getObjectName(), field, rejectedValue, false, new ErrorCode[]{errorCode}, new Object[]{}, errorCode.toDefaultMessage(field, rejectedValue)));
	}

	public void minViolation(String field, int rejectedValue, int min) {
		addFieldError(field, new FieldError(getObjectName(), field, rejectedValue, false, new ErrorCode[]{ErrorCode.FIELD_NUMBER_BELOW_MIN}, new Object[]{}, ErrorCode.FIELD_NUMBER_BELOW_MIN.toDefaultMessage(field, rejectedValue)));
	}
	
	public void bindViolation(String field, String rejectedValue, Class<?> type) {
		addFieldError(field, new FieldError(getObjectName(), field, rejectedValue, true, new ErrorCode[]{ErrorCode.FIELD_WRONG_TYPE}, new Object[]{}, ErrorCode.FIELD_WRONG_TYPE.toDefaultMessage(field, rejectedValue)));
	}
	
	private void addFieldError(String field, FieldError error) {
		List<FieldError> fieldErrorsForField = this.fieldErrors.get(field);
		if (fieldErrorsForField == null) {
			fieldErrorsForField = new ArrayList<FieldError>();
		}
		fieldErrorsForField.add(error);
		fieldErrors.put(field, fieldErrorsForField);
	}

	public void addAllErrors(Errors errors) {
		this.globalErrors.addAll(errors.getGlobalErrors());
		for (FieldError fieldError : errors.getFieldErrors()) {
			addFieldError(fieldError.getField(), fieldError);
		}
	}

	@Override
	public List<FieldError> getFieldErrors() {
		ArrayList<FieldError> fieldErrorsFlat = new ArrayList<FieldError>();
		for (List<FieldError> fieldErrorList : fieldErrors.values()) {
			fieldErrorsFlat.addAll(fieldErrorList);
		}
		return fieldErrorsFlat;
	}

	@Override
	public boolean hasErrors() {
		return !getAllErrors().isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(getErrorCount()).append(" errors");
		for (ObjectError error : getAllErrors()) {
			sb.append('\n').append(error);
		}
		return sb.toString();
	}


	@Override
	public boolean hasFieldErrors(String field) {
		return (getFieldErrorCount(field) > 0);
	}

	public List<ObjectError> getAllErrors() {
		List<ObjectError> result = new LinkedList<ObjectError>();
		result.addAll(getGlobalErrors());
		result.addAll(getFieldErrors());
		return Collections.unmodifiableList(result);
	}

	
	public List<FieldError> getFieldErrors(String field) {
		List<FieldError> fieldErrors = getFieldErrors();
		List<FieldError> result = new LinkedList<FieldError>();
		String fixedField = field;
		for (FieldError error : fieldErrors) {
			if (isMatchingFieldError(fixedField, error)) {
				result.add(error);
			}
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Check whether the given FieldError matches the given field.
	 * @param field the field that we are looking up FieldErrors for
	 * @param fieldError the candidate FieldError
	 * @return whether the FieldError matches the given field
	 */
	private boolean isMatchingFieldError(String field, FieldError fieldError) {
		if (field.equals(fieldError.getField())) {
			return true;
		}
		// Optimization: use charAt and regionMatches instead of endsWith and startsWith (SPR-11304)
		int endIndex = field.length() - 1;
		return (endIndex >= 0 && field.charAt(endIndex) == '*' &&
				(endIndex == 0 || field.regionMatches(0, fieldError.getField(), 0, endIndex)));
	}

	private int getErrorCount() {
		return getAllErrors().size();
	}

	private int getFieldErrorCount(String field) {
		return getFieldErrors(field).size();
	}
}