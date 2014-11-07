package com.gadgetworks.codeshelf.validation;

import javax.validation.ValidationException;

import lombok.Getter;
import lombok.ToString;

@ToString(of={"errors"})
public class InputValidationException extends ValidationException {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	@Getter
	private final Errors errors;

	public InputValidationException(Object instance, String field, Object value, ErrorCode errorCode) {
		DefaultErrors defaultErrors = new DefaultErrors(instance.getClass());
		defaultErrors.rejectValue(field, value, errorCode);
		this.errors = defaultErrors;
	}

	public InputValidationException(Object instance, String customMessage) {
		DefaultErrors defaultErrors = new DefaultErrors(instance.getClass());
		defaultErrors.reject(ErrorCode.GENERAL, customMessage);
		this.errors = defaultErrors;
	}
	
	public InputValidationException(Errors errors) {
		this.errors = errors;
	}
	
	public boolean hasViolationForProperty(String field) {
		return this.errors.hasFieldErrors(field);
	}

}
