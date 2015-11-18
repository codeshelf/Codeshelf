package com.codeshelf.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.codeshelf.model.domain.Facility;

public class AccumulateDailyMetricsProcessor extends SingleBatchProcessorABC{
	private Facility facility;
	
	public AccumulateDailyMetricsProcessor(Facility facility) {
		this.facility = facility;
	}
	
	@Override
	public int doBatch(int batchCount) throws Exception {
		facility = facility.reload();
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		TimeZone facilityTimeZone = facility.getTimeZone();
		Calendar cal = Calendar.getInstance(facilityTimeZone);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, -1);
		String dateStr = out.format(cal.getTime());
		
		facility.computeMetrics(dateStr, true);
		
		setDone(true);;
		return 1;
	}
}
