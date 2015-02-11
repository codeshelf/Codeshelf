/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, Inc., All rights reserved
 *  file TimeFormat.java
 *  Author Jon Ranstrom
 *******************************************************************************/
package com.codeshelf.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

/**
 * The goal is to return useful, compact time representation.
 * Today should show in military hours and minutes as 1715
 * Within the last week as TU2317
 * More than a week old as Month and day, as MAY_07
 * More than a year old as Month and year MAY_2013
 */
public final class TimeFormat {
	// Careful of the disconnect. Same day means same day, back to midnight. WithinWeek literally means back to same time a week ago.
	public enum TimePast {
		TimePastFuture,
		TimePastSameDay,
		TimePastWithinWeek,
		TimePastWithinYear,
		TimePastBeyond1Year
	}

	private TimeFormat() {
	};

	public static String getUITime(Timestamp inTime) {
		if (inTime == null)
			return "";

		TimePast formatToUse = getTimeFormatCategory(inTime);

		if (formatToUse == TimePast.TimePastSameDay) { // a time today as 1527
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
			return formatter.format(inTime);
		} else if (formatToUse == TimePast.TimePastWithinWeek || formatToUse == TimePast.TimePastFuture) { // within the last week as Tue1347
			// future beyond same day. Should not hit this case much. Let's assume TU2317 will be useful. Future to show +TU2317
			SimpleDateFormat formatter = new SimpleDateFormat("EEE HH:mm");
			String returnStr = formatter.format(inTime);
			if (formatToUse == TimePast.TimePastFuture)
				returnStr = "+" + returnStr;
			return returnStr;
		} else if (formatToUse == TimePast.TimePastWithinYear) { // more than week past as MAY_07
			SimpleDateFormat formatter = new SimpleDateFormat("MMM_dd");
			return formatter.format(inTime);
		} else if (formatToUse == TimePast.TimePastBeyond1Year) { // more than year past as May2013
			SimpleDateFormat formatter = new SimpleDateFormat("MMMyyyy");
			return formatter.format(inTime);
		}
		return "";
	}

	// Should use an enum. For now integer: 0: today. 1 within the week. 2 within the year. 3 more than a year
	private static TimePast getTimeFormatCategory(Timestamp inTime) {
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		// same day
		if (DateUtils.isSameDay(inTime, nowTime))
			return TimePast.TimePastSameDay;
		if (inTime.after(nowTime))
			return TimePast.TimePastFuture;

		Date proposedTime = DateUtils.addYears(inTime, 1);
		if (proposedTime.before(nowTime))
			return TimePast.TimePastBeyond1Year;

		proposedTime = DateUtils.addWeeks(inTime, 1);
		if (proposedTime.before(nowTime))
			return TimePast.TimePastWithinWeek;

		return TimePast.TimePastWithinYear;
	}

}
