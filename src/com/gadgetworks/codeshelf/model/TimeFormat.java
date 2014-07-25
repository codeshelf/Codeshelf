/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, Inc., All rights reserved
 *  file TimeFormat.java
 *  Author Jon Ranstrom
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.sql.Timestamp;

/**
 * The goal is to return useful, compact time representation.
 * Today should show in military hours and minutes as 1715
 * Within the last week as TU2317
 * More than a week old as Month and day, as MAY_07
 * More than a year old as Month and year MAY_2013
 */
public class TimeFormat {
	public static String getUITime(Timestamp inTime) {
		return "time";
	}

}
