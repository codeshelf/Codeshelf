package com.gadgetworks.codeshelf.edi;

import java.util.List;

import lombok.ToString;

import com.google.common.collect.Lists;

/**
 * Track if any of the import lines failed and why.
 *
 */
@ToString()
public class ImportResult {
		
	private List<Failure> failedLines;
	
	public ImportResult() {
		failedLines = Lists.newArrayList();
	}
	
	/**
	 * Add failure description when there is no exception
	 */
	public void addFailure(Object failedLine, String errorDescription) {
		this.failedLines.add(new Failure(failedLine, errorDescription, null));
	}
	
	/**
	 * Add failure description when there is an exception
	 */
	public void addFailure(Object failedLine, Exception failure) {
		this.failedLines.add(new Failure(failedLine, failure.toString(), failure));
	}

	/**
	 * Convenience method to determine if there are any failed lines
	 */
	public boolean isSuccessful() {
		return failedLines != null && failedLines.size() == 0;
	}
	
	@ToString()
	private class Failure {
		private Object failedLine;
		private Exception failure;
		private String failureDescription;
		
		public Failure(Object failedLine, String failureDescription, Exception failure) {
			super();
			this.failedLine = failedLine;
			this.failureDescription = failureDescription;
			this.failure = failure;
		}
	}
}
