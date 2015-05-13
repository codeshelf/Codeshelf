package com.codeshelf.util;

import org.apache.commons.lang.exception.ExceptionUtils;

public class CsExceptionUtils {
	public static String exceptionToString(Exception e) { 
		String error = e.getMessage();
		if (error == null || error.isEmpty()) {
			error = ExceptionUtils.getStackTrace(e);
		}
		return e.getClass().getName() + ": " + error + "\n";
	}

}
