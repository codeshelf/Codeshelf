package com.codeshelf.api.responses;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.lang.ObjectUtils;

import com.codeshelf.model.domain.Worker;
import com.google.common.base.Strings;

@EqualsAndHashCode(of={"name", "id"})
public class WorkerDisplay implements Comparable<WorkerDisplay> {
	
	public static final FieldComparator<Map<Object, Object>>	ItemComparator = new FieldComparator<Map<Object, Object>>() {
		@Getter
		private String[] sortedBy = {"count", "name", "id"};
		
	};

	@Getter
	private String name;

	@Getter
	private String id;
	
	public WorkerDisplay(EventDisplay eventDisplay) {
		this.id = Strings.nullToEmpty(eventDisplay.getWorkerId());
		this.name = Strings.nullToEmpty(eventDisplay.getWorkerName());
	}

	public WorkerDisplay(Worker worker) {
		if (worker != null) {
			this.id = Strings.nullToEmpty(worker.getDomainId());
			this.name = Strings.nullToEmpty(worker.getWorkerNameUI());
		} else {
			this.id = "";
			this.name= "";
		}
	}

	@Override
	public int compareTo(WorkerDisplay item) {
		Comparable<?> thisName= this.getName();
		Comparable<?> name = item.getName();
		return ObjectUtils.compare(thisName, name);
	}
}
