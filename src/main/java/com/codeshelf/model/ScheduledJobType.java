package com.codeshelf.model;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.quartz.Job;
import org.quartz.JobKey;

import com.codeshelf.application.FacilitySchedulerService.NotImplementedJob;

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
			"0 01 * * *",
			null,
			true,
			ScheduledJobCategory.METRIC),
	DatabasePurge(DataPurgeJob.class,
			"Purge old data.",
			"0 20 * * *",
			"ParameterSetDataPurge",
			false,
			ScheduledJobCategory.DATABASE,
			ScheduledJobCategory.PURGE),
	DatabaseSizeCheck(NotImplementedJob.class,
			"Gather total data size for health check.",
			"0 21 * * *",
			"ParameterSetDataQuantityHealthCheck",
			true,
			ScheduledJobCategory.DATABASE,
			ScheduledJobCategory.CHECK),
	EdiSizeCheck(NotImplementedJob.class,
			"Check EDI directories for health check.",
			"0 21 * * *",
			"ParameterEdiFreeSpaceHealthCheck",
			false,
			ScheduledJobCategory.EDI,
			ScheduledJobCategory.CHECK),
	EdiPurge(NotImplementedJob.class,
			"Clean old files out of EDI directories",
			"0 01 * * *",
			null,
			false),
			
	PutWallLightRefresher(NotImplementedJob.class,
			"Periodically make sure putwall lights are current.",
			"0 20 * * *",
			null,
			false,
			ScheduledJobCategory.PUTWALL,
			ScheduledJobCategory.REFRESHER),
	EdiImport(NotImplementedJob.class,
			"Check each file-based importer for new files",
			"0 20 * * *",
			null,
			true,
			ScheduledJobCategory.EDI),
	Test(TestJob.class,
				"Test job to test trigger rules",
				"0 20 * * *",
				null,
				false,
				ScheduledJobCategory.TEST);
			

	@Getter
	@Accessors(prefix = "m")
	private Class<? extends Job> mJobClass;
	@Getter
	@Accessors(prefix = "m")
	private String	mJobDescription;
	@Getter
	@Accessors(prefix = "m")
	private String	mDefaultSchedule;
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
		mDefaultSchedule = defaultSchedule;
		mParameterExtensionName = parameterExtensionName;
		mDefaultOnOff = onOff;
		mCategories = categories;
		mKey = new JobKey(this.name());
	}

}
