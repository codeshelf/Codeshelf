package com.gadgetworks.codeshelf.validation;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.ToString;

import com.gadgetworks.codeshelf.edi.OutboundOrderCsvBean;

/**
 * Track if any of the import lines failed and why.
 *
 */
@ToString()
public class BatchResult<T> {
	
	@Getter
	private List<T> result;
	
	@Getter
	private List<Violation> violations;
	
	public BatchResult() {
		this.result = new ArrayList<T>();
		this.violations = new ArrayList<Violation>();
	}

	public void add(T inResult) {
		this.result.add(inResult);
	}

	public void addViolation(Object relatedContextObject, String errorMsg) {
		this.violations.add(new Violation(relatedContextObject, errorMsg));
	}

	public void addLineViolation(int lineCount, Object orderBean, String errorMsg) {
		this.violations.add(new Violation(orderBean, errorMsg));
	}

	public void addLineViolation(int lineCount, OutboundOrderCsvBean orderBean, Exception e) {
		this.violations.add(new Violation(orderBean, e));
	}

	public boolean isSuccessful() {
		return (getViolations() == null) || getViolations().isEmpty();
	}

	public void addAllViolations(BatchResult<?> otherResult) {
		this.violations.addAll(otherResult.getViolations());
	}



}
