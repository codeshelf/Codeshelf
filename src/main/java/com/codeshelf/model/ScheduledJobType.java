package com.codeshelf.model;

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

	AccumulateDailyMetricsJob(
			"AccumulateDailyMetrics",
			"Summarize previous day's completed work instructions.",
			"0 01 * * *",
			null,
			true),
	DataPurgeJob("DataPurge", "Purge old data.", "0 20 * * *", "ParameterSetDataPurge", false),
	DataSizeCheckJob(
			"DataSizeCheck",
			"Gather total data size for health check.",
			"0 21 * * *",
			"ParameterSetDataQuantityHealthCheck",
			true),
	EdiSpaceCheckJob(
			"EdiSpaceCheck",
			"Check EDI directories for health check.",
			"0 21 * * *",
			"ParameterEdiFreeSpaceHealthCheck",
			false),
	EdiDirectoriesPurgeJob("EdiDirectoriesPurgeJ", "Clean old files out of EDI directories", "0 01 * * *", null, false),
	PutWallLightRefresherJob(
			"PutWallLightRefresher",
			"Periodically make sure putwall lights are current.",
			"0 20 * * *",
			null,
			false),
	EdiImportJob("EdiImport", "Check each file-based importer for new files", "0 20 * * *", null, true);

	@Getter
	@Accessors(prefix = "m")
	private String	mJobName;
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

	ScheduledJobType(String jobName, String jobDescription, String defaultSchedule, String parameterExtensionName, boolean onOff) {
		mJobName = jobName;
		mJobDescription = jobDescription;
		mDefaultSchedule = defaultSchedule;
		mParameterExtensionName = parameterExtensionName;
		mDefaultOnOff = onOff;
	}

}
