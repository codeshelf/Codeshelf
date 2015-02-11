package com.gadgetworks.codeshelf.validation;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.ToString;

/**
 * Track if any of the import lines failed and why.
 *
 */
@ToString()
public class BatchResult<T> {
	
	@Getter
	private List<T> result;
	
	@Getter
	private List<FieldError> violations;
	
	public BatchResult() {
		this.result = new ArrayList<T>();
		this.violations = new ArrayList<FieldError>();
	}

	public void add(T inResult) {
		this.result.add(inResult);
	}

	public void addViolation(String field, Object relatedContextObject, ErrorCode errorCode) {
		this.violations.add(new FieldError(getRootObjectName(), field, relatedContextObject, errorCode));
	}

	public void addViolation(String field, Object relatedContextObject, String errorMsg) {
		this.violations.add(new FieldError(getRootObjectName(), field, relatedContextObject, errorMsg));
	}

	public void addLineViolation(int lineCount, Object relatedContextObject, String errorMsg) {
		this.violations.add(new FieldError(getRootObjectName(), String.valueOf(lineCount), relatedContextObject, errorMsg));
	}

	public void addLineViolation(int lineCount, Object relatedContextObject, Exception e) {
		this.violations.add(new FieldError(getRootObjectName(), String.valueOf(lineCount), relatedContextObject, e.toString()));
	}

	public boolean isSuccessful() {
		return (getViolations() == null) || getViolations().isEmpty();
	}
	
	private String getRootObjectName() {
		return "";
	}
}
