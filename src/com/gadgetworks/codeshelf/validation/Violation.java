package com.gadgetworks.codeshelf.validation;

import lombok.Getter;
import lombok.ToString;

@ToString() 
public class Violation {
	
	@Getter
	private Object failedLine;
	
	@Getter
	private Exception failure;
	
	@Getter
	private String failureDescription;
	
	public Violation(Object failedLine, String failureDescription) {
		super();
		this.failedLine = failedLine;
		this.failureDescription = failureDescription;
	}

	public Violation(Object inRelatedObject, Exception inException) {
		this.failedLine = inRelatedObject;
		this.failureDescription = inException.toString();
		this.failure = inException;
	}
	
	public Violation(Object inRelatedObject, Exception inException, String furtherDetail) {
		this.failedLine = inRelatedObject;
		this.failureDescription = furtherDetail;
		this.failure = inException;
	}
	
	
}