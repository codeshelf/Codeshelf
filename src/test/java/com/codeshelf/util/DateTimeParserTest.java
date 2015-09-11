package com.codeshelf.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.codeshelf.testframework.MinimalTest;

public class DateTimeParserTest extends MinimalTest {

	private DateTimeParser	parser;

	@Before
	public void init() {
		parser = new DateTimeParser();
	}

	@Test
	public void testDaysAgoString() {
		Date today = new Date(System.currentTimeMillis());
		Date date = parseSingleDate("2 Days Ago");
		Assert.assertTrue(date.before(today));
		Assert.assertEquals(2, new Period(new DateTime(date), new DateTime(today)).getDays());
	}
	
	@Test
	public void testYearMonthDay24HrTimes() {
		validateDateTime("2012-09-26 11:31:03", 2012, 9, 26, 11, 31, 03, TimeZone.getDefault());
		validateDateTime("2012-09-26 13:31:03", 2012, 9, 26, 13, 31, 03, TimeZone.getDefault());
		validateDateTime("2012-09-26 13:31", 2012, 9, 26, 13, 31, 00, TimeZone.getDefault());
		validateDateTime("2012-01-01 00:00:00", 2012, 1, 1, 0, 0, 0, TimeZone.getDefault());
	}

	@Test
	public void testUSDayMonthYear24hourTimes() {
		validateDateTime("6/25/14 12:00:03", 2014, 6, 25, 12, 00, 03, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00", 2014, 6, 25, 12, 00, 00, TimeZone.getDefault());
	}

	@Test
	public void testUSYearMonthDayMeridianTimes() {
		validateDateTime("6/25/14 12:00:00A", 2014, 6, 25, 00, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00:00P", 2014, 6, 25, 12, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00A", 2014, 6, 25, 00, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00P", 2014, 6, 25, 12, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00a", 2014, 6, 25, 00, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00p", 2014, 6, 25, 12, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00am", 2014, 6, 25, 00, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00pm", 2014, 6, 25, 12, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00AM", 2014, 6, 25, 00, 00, 00, TimeZone.getDefault());
		validateDateTime("6/25/14 12:00PM", 2014, 6, 25, 12, 00, 00, TimeZone.getDefault());
	}

	@Test
	public void testISOUTC() {
		@SuppressWarnings("unused")
		Calendar cal = validateDateTime("2015-06-15T07:00:00.000Z", 2015, 6, 15, 7, 0, 0, TimeZone.getTimeZone("UTC"));
	}

	
	@Test
	public void testDateParseExceptions() {
		// No uncaught throws happen. Returns a date or not.
		Date theDate = null;
		// first two result in a null data returned
		theDate = parseSingleDate("");
		Assert.assertNull(theDate);
		theDate = parseSingleDate("a");
		Assert.assertNull(theDate);
		
		// next ones return something. Not meaningful!
		theDate = parseSingleDate("9"); // parses to 9AM local time today.
		Assert.assertNotNull(theDate);
		theDate = parseSingleDate("-3"); // parses to 3AM local time today. (3 would also)
		Assert.assertNotNull(theDate);
		Assert.assertNotNull(theDate);
		theDate = parseSingleDate("0");// parses to 00:00 (midnight) local time today.
		Assert.assertNotNull(theDate);
	}

	private Date parseSingleDate(String inValue) {
		return parser.parse(inValue);
	}

	/**
	 * 
	 * Asserts that the given string value parses down to the given 
	 * month, day, year, hours, minutes, and seconds
	 * 
	 */
	private Calendar validateDateTime(String value, int year, int month, int day, int hours, int minutes, int seconds, TimeZone timeZone) {

		Date date = parseSingleDate(value);
		return validateDateTime(date, year, month, day, hours, minutes, seconds, timeZone);
	}

	/**
	 * Asserts that the given date contains the given attributes
	 * 
	 */
	private Calendar validateDateTime(Date date, int year, int month, int day, int hours, int minutes, int seconds, TimeZone timeZone) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(timeZone);
		calendar.setTime(date);
		Assert.assertEquals(year, calendar.get(Calendar.YEAR));
		Assert.assertEquals(month - 1, calendar.get(Calendar.MONTH));
		Assert.assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH));
		Assert.assertEquals(hours, calendar.get(Calendar.HOUR_OF_DAY));
		Assert.assertEquals(minutes, calendar.get(Calendar.MINUTE));
		Assert.assertEquals(seconds, calendar.get(Calendar.SECOND));
		return calendar;
	}

}
