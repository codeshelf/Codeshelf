package com.codeshelf.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;

import com.codeshelf.model.domain.ScheduledJob;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.google.common.collect.ComparisonChain;

import lombok.Getter;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
public class ScheduledJobView {
	
	public static final Comparator<ScheduledJobView> SORT_BY_TYPE = new Comparator<ScheduledJobView>() {

		@Override
		public int compare(ScheduledJobView o1, ScheduledJobView o2) {
			return ComparisonChain.start()
				.compareTrueFirst(o1 != null, o2 != null)
				.compare(o1.getType().name(), o2.getType().name())
				.result();
		}

	};


	
	@Getter
	ScheduledJobType type;
	String cronExpression;
	List<Date> futureScheduled;
	boolean running;
	boolean active;
	
	ScheduledJobView(ScheduledJob job, boolean running) {
		this.type = job.getType();
		this.cronExpression = job.getCronExpression().getCronExpression();
		futureScheduled = new ArrayList<Date>();
		Date lastDate = DateTime.now().toDate();
		for(int i = 0; i < 3; i++) {
			lastDate = job.getCronExpression().getNextValidTimeAfter(lastDate);
			futureScheduled.add(lastDate);
		}
		this.running = running;
		this.active = job.isActive();
	}
}