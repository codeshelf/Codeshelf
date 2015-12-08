package com.codeshelf.api.responses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import lombok.Getter;

public class ResultDisplay<T> {

	private final Long explicitTotal;
	
	@Getter
	private String[] sortedBy;
	
	private Collection<T> results;
	
	public ResultDisplay() {
		this(0);
	}
	
	public ResultDisplay(long explicitTotal) {
		this(explicitTotal, new ArrayList<T>());
	}
	
	public ResultDisplay(FieldComparator<T> comparator) {
		this(null, new TreeSet<>(comparator));
		this.sortedBy = comparator.getSortedBy();
	}
	
	public ResultDisplay(Long total, Collection<T> resultSet) {
		this.explicitTotal = total;
		this.results = resultSet;
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
	
	public void add(T value) {
		@SuppressWarnings("unused")
		boolean ret = results.add(value);
	}


	public void addAll(Collection<T> values) {
		@SuppressWarnings("unused")
		boolean ret = results.addAll(values);
		
	}

}
