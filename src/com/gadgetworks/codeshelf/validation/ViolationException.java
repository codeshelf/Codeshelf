package com.gadgetworks.codeshelf.validation;


public class ViolationException extends Exception {

	private Object contextObject;
	private String errorMsg;
	
	public ViolationException(Object contextObject, String errorMsg) {
		super(errorMsg);
		this.contextObject = contextObject;
	}

}
