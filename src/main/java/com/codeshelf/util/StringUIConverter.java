package com.codeshelf.util;

import java.text.DecimalFormat;


/**
 * A utility for the conversions we do java-side to support the UI
 * 
 */
public class StringUIConverter {
	
	public StringUIConverter() {
		
	}

	final public static char[] HEXADECIMAL_CHARS="0123456789abcdef".toCharArray();
	public static String bytesToHexString(byte[] raw) {
	    char[] result = new char[raw.length * 3];
	    for(int ix=0; ix < raw.length; ix++) {
			result[3*ix]=HEXADECIMAL_CHARS[(raw[ix]&0xff) >> 4];
			result[3*ix +1]=HEXADECIMAL_CHARS[raw[ix] & 0x0F];
			result[3*ix +2]=' ';
	    }
	    return new String(result);
	}

	public static byte[] hexStringToBytes(String s) {
	    int len = s.length();
	    if(len%2 != 0) {
	    	throw new NumberFormatException("invalid hex string length "+len);
	    }
	    byte[] result = new byte[len/2];
	    for(int i=0; i<len; i+=2) {
	        result[i/2] = (byte)((Character.digit(s.charAt(i), 16) * 16)
	        		+ Character.digit(s.charAt(i+1), 16));
	    }
	    return result;
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
