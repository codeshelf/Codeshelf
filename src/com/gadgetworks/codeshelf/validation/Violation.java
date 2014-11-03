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
	private String description;
	
	public Violation(Object failedLine, String failureDescription) {
		super();
		this.failedLine = failedLine;
		this.description = failureDescription;
	}

	public Violation(Object inRelatedObject, Exception inException) {
		this.failedLine = inRelatedObject;
		this.description = inException.toString();
		this.failure = inException;
	}
	
	public Violation(Object inRelatedObject, Exception inException, String furtherDetail) {
		this.failedLine = inRelatedObject;
		this.description = furtherDetail;
		this.failure = inException;
	}
	
	
}