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

	public InputValidationException(Object instance, String field, String errorCode) {
		DefaultErrors defaultErrors = new DefaultErrors(instance.getClass());
		defaultErrors.rejectValue(field, errorCode);
		this.errors = defaultErrors;
	}
	public InputValidationException(Errors errors) {
		this.errors = errors;
	}
	
	public boolean hasViolationForProperty(String field) {
		return this.errors.hasFieldErrors(field);
	}

}
