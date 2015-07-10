package com.codeshelf.api.responses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.codeshelf.service.NotificationService.WorkerEventTypeGroup;

import lombok.Getter;

public class ResultDisplay {

	private final Integer explicitTotal;
	
	@Getter
	private String[] sortedBy;
	
	@Getter
	private Collection<Object> results;
	
	public ResultDisplay(FieldComparator<Map<Object,Object>> comparator) {
		this(null, new TreeSet<>(comparator));
		this.sortedBy = comparator.getSortedBy();
	}

	
	public ResultDisplay(int explicitTotal) {
		this(explicitTotal, new ArrayList<Map<Object, Object>>());
	}
	
	public ResultDisplay() {
		this(null, new ArrayList<Map<Object, Object>>());
	}

	
	private ResultDisplay(Integer total, Collection<Map<Object, Object>> resultSet) {
		this.explicitTotal = total;
		this.results = new ArrayList<Object>(resultSet);
	}

	public int getTotal() {
		if(explicitTotal != null) {
			return explicitTotal;
		} else {
			return results.size();
		}
	}
	
	public void add(Map<Object, Object> values) {
		@SuppressWarnings("unused")
		boolean ret = results.add(values);
	}


	public void addAll(List<?> values) {
		@SuppressWarnings("unused")
		boolean ret = results.addAll(values);
		
	}

}
