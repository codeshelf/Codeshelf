package com.codeshelf.api.responses;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;

public class ResultDisplay {

	private final Integer explicitTotal;
	
	@Getter
	private String[] sortedBy;
	
	@Getter
	private Set<Map<Object, Object>> results;
	
	public ResultDisplay(FieldComparator<Map<Object,Object>> comparator) {
		this(null, new TreeSet<>(comparator));
		this.sortedBy = comparator.getSortedBy();
	}
	
	public ResultDisplay(int explicitTotal) {
		this(explicitTotal, new HashSet<Map<Object, Object>>());
	}
	
	private ResultDisplay(Integer total, Set<Map<Object, Object>> resultSet) {
		this.explicitTotal = total;
		this.results = resultSet;
	}

	public int getTotal() {
		if(explicitTotal != null) {
			return explicitTotal;
		} else {
			return results.size();
		}
	}
	
	public void add(Map<Object, Object> values) {
		results.add(values);
	}

}
