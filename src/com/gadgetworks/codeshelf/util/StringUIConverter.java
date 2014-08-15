package com.gadgetworks.codeshelf.util;

import java.text.DecimalFormat;


/**
 * A utility for the conversions we do java-side to support the UI
 * 
 */
public class StringUIConverter {
	
	public StringUIConverter() {
		
	}
	
	public static String doubleToTwoDecimalsString(Double inDouble) {
		// Most of our internals are Double floats, distance in meters.  Raw UI fetch of that looks bad.
		if (inDouble == null || inDouble == 0.0)
			return "0";
		else {
			DecimalFormat df = new DecimalFormat("#.##");      
			Double rounded = Double.valueOf(df.format(inDouble));
			return rounded.toString();
		}
	}
}
