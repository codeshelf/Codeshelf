package com.codeshelf.util;

import java.sql.Timestamp;
import java.util.Calendar;

public class TimeUtils {
	public static final long MILLISECOUNDS_IN_MINUTE = 1000 * 60;
	public static final long MILLISECOUNDS_IN_HOUR = MILLISECOUNDS_IN_MINUTE * 60;
	public static final long MILLISECOUNDS_IN_DAY = MILLISECOUNDS_IN_HOUR * 24;
	
	public static Timestamp truncateTimeToHour(Timestamp in) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(in);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Timestamp out = new Timestamp(cal.getTimeInMillis());
		return out;
	}
	
	public static Timestamp addTime(Timestamp t, long increment){
		return new Timestamp(t.getTime() + increment);
	}
}
