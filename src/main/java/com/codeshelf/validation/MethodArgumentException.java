package com.gadgetworks.codeshelf.validation;

import javax.validation.ValidationException;

import lombok.Getter;
import lombok.ToString;


/**
 * Primarily for service methods that are being called and the argument violation needs to be described by
 * position
 * 
 * @author pmonteiro
 *
 */
@ToString
public class MethodArgumentException extends ValidationException {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	@Getter
	private int	parameterPosition;
	
	@Getter
	private String	argumentValue;
	
	@Getter
	private ErrorCode	errorCode;

	public MethodArgumentException(int parameterPosition, String argumentValue, ErrorCode errorCode) {
		super();
		this.parameterPosition = parameterPosition;
		this.argumentValue = argumentValue;
		this.errorCode = errorCode;
	}

}
