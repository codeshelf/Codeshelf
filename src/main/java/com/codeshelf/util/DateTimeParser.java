package com.codeshelf.util;

import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

/**
 * TODO doc all the types of strings this can parse but it should match
 * @see http://natty.joestelmach.com
 * 
 */
public class DateTimeParser {
	
	public enum UnspecifiedTime {
		START_OF_DAY,
		END_OF_DAY
	}
	
	private Parser mparser;
	private DateTimeFormatter isoFormatter; // for iso times
	
	public DateTimeParser() {
		mparser = new Parser();
		isoFormatter = ISODateTimeFormat.dateTimeParser();
	}
	
	synchronized public Date parse(String inDateString) {
		return parse(inDateString, UnspecifiedTime.START_OF_DAY);
	}

	synchronized public Date parse(String inDateString, UnspecifiedTime unspecifiedTime) {
		try {
			DateTime dateTime = isoFormatter.parseDateTime(inDateString);
			return dateTime.toDate();
		} catch(Exception e) {
			//try the other less strict pareser
		}
		List<DateGroup> dateGroups = mparser.parse(inDateString);
		if (dateGroups.isEmpty()) {
			return null;
		}
		DateGroup dateGroup = dateGroups.get(0);
		List<Date> dates = dateGroup.getDates();
		if (dates.isEmpty()) {
			return null;
		}
		Date baseDate = dates.get(0); 
		if (dateGroup.isTimeInferred()) {
			DateTime dt = new DateTime(baseDate);
			if (UnspecifiedTime.START_OF_DAY.equals(unspecifiedTime)) {
				return dt.withTimeAtStartOfDay().toDate();
			} else {
				return dt.withTime(23,59,59,999).toDate();
			}
		} else {
			return baseDate;
		}
	}

}
