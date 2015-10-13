package com.codeshelf.validation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Track if any of the import lines failed and why.
 *
 */
@ToString()
public class BatchResult<T> {
	
	@Getter
	private List<T> result;
	
	@Getter
	@Setter
	private Date received;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Date completed;
	
	@Getter
	@Setter
	@JsonProperty
	int ordersProcessed = 0;

	@Getter
	@Setter
	@JsonProperty
	int linesProcessed = 0;
	
	@Getter
	@Setter
	@JsonProperty
	List<String> orderIds = new ArrayList<>();

	@Getter
	@Setter
	@JsonProperty
	List<String> itemIds = new ArrayList<>();

	@Getter
	@Setter
	@JsonProperty
	List<String> gtins = new ArrayList<>();

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

	public void merge(BatchResult<T> batchResult) {
		this.result.addAll(batchResult.getResult());
	}

}
