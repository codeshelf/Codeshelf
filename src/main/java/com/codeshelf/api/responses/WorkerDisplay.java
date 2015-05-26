package com.codeshelf.api.responses;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.lang.ObjectUtils;

import com.google.common.base.Strings;

@EqualsAndHashCode(of={"name", "id"})
public class WorkerDisplay implements Comparable<WorkerDisplay> {
	
	public static final FieldComparator<Map<Object, Object>>	ItemComparator = new FieldComparator<Map<Object, Object>>() {

		@Getter
		private String[] sortedBy = {"name", "id"};
		
		@Override
		public int compare(Map<Object, Object> record1, Map<Object, Object> record2) {
			int value = 0;
			
			for (String sortField : sortedBy) {
				Comparable<?> item1 = (Comparable<?>) record1.get(sortField);
				Comparable<?> item2 = (Comparable<?>) record2.get(sortField);
				value =  ObjectUtils.compare(item1, item2);
				if (value != 0) {
					break;
				}
			}
			return value;
		}
		
	};

	@Getter
	private String name;

	@Getter
	private String id;
	
	public WorkerDisplay(EventDisplay eventDisplay) {
		this.id = Strings.nullToEmpty(eventDisplay.getWorkerId());
		this.name = Strings.nullToEmpty(eventDisplay.getWorkerName());
	}

	@Override
	public int compareTo(WorkerDisplay item) {
		Comparable<?> thisName= this.getName();
		Comparable<?> name = item.getName();
		return ObjectUtils.compare(thisName, name);
	}
}
