package com.codeshelf.model;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.text.ParseException;

import org.quartz.CronExpression;
import org.quartz.Job;
import org.quartz.JobKey;

import com.codeshelf.application.FacilitySchedulerService.NotImplementedJob;
import com.google.common.base.Optional;

/**
 * The kinds of jobs we schedule. In general, only one job of each kind can run at a time at one facility.
 * Notice that we can change the description and/or default schedule values in code here.
 * And also whether it defaults on or off. The actual cron job for a new facility should default to these values.
 * 
 * @author jon ranstrom
 */
public enum ScheduledJobType {

	AccumulateDailyMetrics(NotImplementedJob.class,
			"Summarize previous day's completed work instructions.",
			"0 0 1 * * ?",
			null,
			true,
			ScheduledJobCategory.METRIC),
	DatabasePurge(DataPurgeJob.class,
			"Purge old data.",
			"0 0 2 * * ?",
			"ParameterSetDataPurge",
			true,
			ScheduledJobCategory.DATABASE,
			ScheduledJobCategory.PURGE),
	DatabaseSizeCheck(NotImplementedJob.class,
			"Gather total data size for health check.",
			"0 0 3 * * ?",
			"ParameterSetDataQuantityHealthCheck",
			true,
			ScheduledJobCategory.DATABASE,
			ScheduledJobCategory.CHECK),
	EdiSizeCheck(NotImplementedJob.class,
			"Check EDI directories for health check.",
			"0 0 4 * * ?",
			"ParameterEdiFreeSpaceHealthCheck",
			false,
			ScheduledJobCategory.EDI,
			ScheduledJobCategory.CHECK),
	EdiPurge(NotImplementedJob.class,
			"Clean old files out of EDI directories",
			"0 0 5 * * ?",
			null,
			false,
			ScheduledJobCategory.EDI,
			ScheduledJobCategory.PURGE
	),
			
	PutWallLightRefresher(NotImplementedJob.class,
			"Periodically make sure putwall lights are current.",
			"0 0 6 * * ?",
			null,
			false,
			ScheduledJobCategory.PUTWALL,
			ScheduledJobCategory.REFRESHER),
	EdiImport(NotImplementedJob.class,
			"Check each file-based importer for new files",
			"0 0 7 * * ?",
			null,
			false, //TODO implement job
			ScheduledJobCategory.EDI),
	Test(TestJob.class,
			"Test job to test trigger rules",
			"0 0 3 * * ?",
			null,
			false,
			ScheduledJobCategory.TEST);
			

	@Getter
	@Accessors(prefix = "m")
	private Class<? extends Job> mJobClass;
	@Getter
	@Accessors(prefix = "m")
	private String	mJobDescription;

	@Accessors(prefix = "m")
	private CronExpression	mDefaultSchedule;
	@Getter
	@Accessors(prefix = "m")
	private String	mParameterExtensionName;
	@Getter
	@Accessors(prefix = "m")
	private boolean	mDefaultOnOff;
	@Getter
	@Accessors(prefix = "m")
	private ScheduledJobCategory[]	mCategories;

	@Getter
	@Accessors(prefix = "m")
	private JobKey mKey;

	
	ScheduledJobType(Class<? extends Job> jobClass, String jobDescription, String defaultSchedule, String parameterExtensionName, boolean onOff, ScheduledJobCategory... categories) {
		mJobClass = jobClass;
		mJobDescription = jobDescription;
		try {
			mDefaultSchedule = new CronExpression(defaultSchedule);
		} catch (ParseException e) {
			throw new RuntimeException("Unable to parse cron expression: " + defaultSchedule, e);
		}
		mParameterExtensionName = parameterExtensionName;
		mDefaultOnOff = onOff;
		mCategories = categories;
		mKey = new JobKey(this.name());
	}

	public CronExpression getDefaultSchedule() {
		return mDefaultSchedule;
	}
	
	public static Optional<ScheduledJobType> findByKey(JobKey jobKey) {
		for (ScheduledJobType type : values()) {
			if (type.getKey().equals(jobKey)) {
				return Optional.of(type);
			}
		}
		return Optional.absent();
	}

}
