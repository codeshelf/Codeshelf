package com.codeshelf.api.responses;

import java.util.Collection;

import lombok.Getter;

public class ResultDisplay<T> {

	private final Long explicitTotal;
	
	@Getter
	private String[] sortedBy;
	
	private Collection<T> results;

	@Getter
	private String next;
	
	public ResultDisplay(Long total, Collection<T> resultSet) {
		this.explicitTotal = total;
		this.results = resultSet;
	}

	public ResultDisplay(Long total, Collection<T> resultSet, String nextToken) {
		this.explicitTotal = total;
		this.results = resultSet;
		this.next = nextToken;
	}

	public ResultDisplay(Collection<T> resultSet) {
		this(new Long(resultSet.size()), resultSet);
	}

	public long getTotal() {
		if(explicitTotal != null) {
			return explicitTotal;
		} else {
			return results.size();
		}
	}
	
	/**
	 * The size of the actual results, not the total possible for the result
	 */
	public int size() {
		return results.size();
	}
	
	public Collection<T> getResults() {
		return results;
	}
}
