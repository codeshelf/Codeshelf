package com.gadgetworks.codeshelf.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class DefaultErrors extends AbstractErrors {

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

	@Override
	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		this.globalErrors.add(new ObjectError(getObjectName(), new String[]{errorCode}, errorArgs, defaultMessage));
		
	}

	@Override
	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		addFieldError(field, new FieldError(getObjectName(), field, null, false, new String[]{errorCode}, errorArgs, defaultMessage));
	}
	
	private void addFieldError(String field, FieldError error) {
		List<FieldError> fieldErrorsForField = this.fieldErrors.get(field);
		if (fieldErrorsForField == null) {
			fieldErrorsForField = new ArrayList<FieldError>();
		}
		fieldErrorsForField.add(error);
		fieldErrors.put(field, fieldErrorsForField);
	}

	@Override
	public void addAllErrors(Errors errors) {
		this.globalErrors.addAll(errors.getGlobalErrors());
		for (FieldError fieldError : errors.getFieldErrors()) {
			addFieldError(fieldError.getField(), fieldError);
		}
	}

	@Override
	public Object getFieldValue(String field) {
		return null;
	}

	@Override
	public List<FieldError> getFieldErrors() {
		ArrayList<FieldError> fieldErrorsFlat = new ArrayList<FieldError>();
		for (List<FieldError> fieldErrorList : fieldErrors.values()) {
			fieldErrorsFlat.addAll(fieldErrorList);
		}
		return fieldErrorsFlat;
	}
	
}