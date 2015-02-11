package com.codeshelf.util;

public final class ThreadUtils {

	private ThreadUtils() {
	}
	
	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} 
		catch (InterruptedException e) {
		}
	}

}
