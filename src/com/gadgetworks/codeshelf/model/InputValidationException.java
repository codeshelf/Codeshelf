package com.gadgetworks.codeshelf.model;

import java.util.Set;

import javax.validation.ValidationException;

import lombok.ToString;

@ToString(of={"inputValidations"})
public class InputValidationException extends ValidationException {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	
	private Set<InputValidation<?>> inputValidations;
	
	public InputValidationException(Set<InputValidation<?>> violations) {
		this.inputValidations = violations;
	}
	
	public boolean hasViolationForProperty(String string) {
		for (InputValidation<?> inputValidation : inputValidations) {
			boolean found = inputValidation.getProperty().equals(string);
			if (found) return true;
		}
		return false;
	
	}

	
}
