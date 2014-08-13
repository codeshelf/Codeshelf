package com.gadgetworks.codeshelf.util;

import java.util.Date;
import java.util.List;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

/**
 * TODO doc all the types of strings this can parse but it should match
 * @see http://natty.joestelmach.com
 * 
 */
public class DateTimeParser {
	
	public DateTimeParser() {
		
	}
	
	public Date parse(String inDateString) {
		Parser parser = new Parser();
		List<DateGroup> dateGroups = parser.parse(inDateString);
		if (dateGroups.isEmpty()) {
			return null;
		}
		DateGroup dateGroup = dateGroups.get(0);
		List<Date> dates = dateGroup.getDates();
		if (dates.isEmpty()) {
			return null;
		}
		return dates.get(0);	
	}
}
