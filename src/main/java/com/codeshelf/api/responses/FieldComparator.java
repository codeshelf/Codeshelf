package com.codeshelf.api.responses;

import java.util.Comparator;

public interface FieldComparator<T> extends Comparator<T>{

	public String[] getSortedBy();
}
