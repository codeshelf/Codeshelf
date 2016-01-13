package com.codeshelf.scheduler;

import java.text.ParseException;
import java.util.TimeZone;

import org.quartz.CronExpression;
import org.quartz.Job;
import org.quartz.JobKey;

import com.codeshelf.application.FacilitySchedulerService.NotImplementedJob;
import com.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.codeshelf.metrics.DataQuantityHealthCheck;
import com.codeshelf.metrics.DatabaseConnectionHealthCheck;
import com.codeshelf.metrics.DropboxGatewayHealthCheck;
import com.codeshelf.metrics.EdiHealthCheck;
import com.codeshelf.metrics.IsProductionServerHealthCheck;
import com.codeshelf.metrics.PicksActivityHealthCheck;
import com.codeshelf.model.TestJob;
import com.google.common.base.Optional;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The kinds of jobs we schedule. In general, only one job of each kind can run at a time at one facility.
 * Notice that we can change the description and/or default schedule values in code here.
 * And also whether it defaults on or off. The actual cron job for a new facility should default to these values.
 * 
 * @author jon ranstrom
 */
public enum ScheduledJobType {
	//Please add new tasks to https://codeshelf.atlassian.net/wiki/display/TD/Health+Checks+And+Other+Scheduled+tasks
	AccumulateDailyMetrics(AccumulateDailyMetricsJob.class,
			"Summarize previous day's completed work instructions.",
			"0 15 0 * * ?",
			true,
			false,
			ScheduledJobCategory.METRIC),
	CheckActiveSiteControllerHealth(ActiveSiteControllerHealthCheck.class,
			"Check Active Site Controllers.",
			"0 */2 * * * ?",
			true,
			true,
			ScheduledJobCategory.CHECK),
	DatabaseConnection(DatabaseConnectionHealthCheck.class,
			"Test database connection.",
			"0 */2 * * * ?",
			true,
			true,
			ScheduledJobCategory.CHECK),
	DatabasePurge(DataPurgeJob.class,
			"Purge old data.",
			"0 0 1 * * ?",
			true,
			false,
			ScheduledJobCategory.DATABASE,
			ScheduledJobCategory.PURGE),
	EdiFilesPurge(EdiFilesPurgeJob.class,
			"Purge old data.",
			"0 0 1 * * ?",
			true,
			false,
			ScheduledJobCategory.PURGE),
	DatabaseSizeCheck(DataQuantityHealthCheck.class,
			"Gather total data size for health check.",
			"0 0 4 * * ?",
			true,
			true,
			ScheduledJobCategory.DATABASE,
			ScheduledJobCategory.CHECK),
	DropboxCheck(DropboxGatewayHealthCheck.class,
			"Test Dropbox connection.",
			"0 */10 * * * ?",
			true,
			true,
			ScheduledJobCategory.CHECK),
	EdiSizeCheck(EdiHealthCheck.class,
			"Check EDI directories for health check.",
			"0 */30 * * * ?",
			false,
			true,
			ScheduledJobCategory.EDI,
			ScheduledJobCategory.CHECK),
	EdiPurge(NotImplementedJob.class,
			"Clean old files out of EDI directories.",
			"0 0 2 * * ?",
			false,
			false,
			ScheduledJobCategory.EDI,
			ScheduledJobCategory.PURGE),
	IsProductionServer(IsProductionServerHealthCheck.class,
			"Check if Production Property is Set.",
			"0 */10 * * * ?",
			false,
			true,
			ScheduledJobCategory.CHECK),
	PicksActivity(PicksActivityHealthCheck.class,
			"Check Picks Activity.",
			"0 */15 * * * ?",
			true,
			true,
			ScheduledJobCategory.CHECK,
			ScheduledJobCategory.DATABASE),
	PutWallLightRefresher(NotImplementedJob.class,
			"Periodically make sure putwall lights are current.",
			"0 6 0 * * ?",
			false,
			true,
			ScheduledJobCategory.PUTWALL,
			ScheduledJobCategory.REFRESHER),
	EdiImport(NotImplementedJob.class,
			"Check each file-based importer for new files",
			"0 7 0 * * ?",
			false, //TODO implement job
			false,
			ScheduledJobCategory.EDI),
	Test(TestJob.class,
			"Test job to test trigger rules",
			"5 0 0 * * ?",
			false,
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

	@Getter
	@Accessors(prefix = "m")
	private boolean mTriggerOnEnable;
	
	ScheduledJobType(Class<? extends Job> jobClass, String jobDescription, String defaultSchedule, boolean onOff, boolean triggerOnEnable, ScheduledJobCategory... categories) {
		mJobClass = jobClass;
		mJobDescription = jobDescription;
		try {
			mDefaultSchedule = new CronExpression(defaultSchedule);
		} catch (ParseException e) {
			throw new RuntimeException("Unable to parse cron expression: " + defaultSchedule, e);
		}
		mDefaultOnOff = onOff;
		mTriggerOnEnable = triggerOnEnable;
		mCategories = categories;
		mKey = new JobKey(this.name());
	}

	public CronExpression getDefaultSchedule(TimeZone timeZone) {
		try {
			CronExpression rightTimeZone = new CronExpression(mDefaultSchedule.getCronExpression());
			rightTimeZone.setTimeZone(timeZone);
			return rightTimeZone;
		} catch (ParseException e) {
			throw new RuntimeException("Unable to parse cron expression: " + mDefaultSchedule, e);
		}
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
