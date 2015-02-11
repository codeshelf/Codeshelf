package com.codeshelf.util;

public class TestUtil {

	
	public static String toDoubleQuote(String simpleJSONSyntax) {
		return simpleJSONSyntax.replaceAll("'", "\"");
		
	}
}
