package com.codeshelf.api.responses;

import java.util.Comparator;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;

public abstract class FieldComparator<T> implements Comparator<T>{

	public abstract String[] getSortedBy();
	
	private static final int DESC = -1;
	
	private int sortOrder = DESC;
	
	public int compare(Map<Object, Object> record1, Map<Object, Object> record2) {
		int value = 0;
		
		
		for (String sortField : getSortedBy()) {
			Comparable<?> item1 = (Comparable<?>) record1.get(sortField);
			Comparable<?> item2 = (Comparable<?>) record2.get(sortField);
			value =  ObjectUtils.compare(item1, item2) * sortOrder;
			if (value != 0) {
				break;
			}
		}
		return value;
	}

}
