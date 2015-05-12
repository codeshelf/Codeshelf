package com.codeshelf.api;

import java.util.HashMap;
import java.util.UUID;

public class PickScriptCallPool {
	private static final int DEF_TIMEOUT_MIN = 5;
	private static HashMap<UUID, String> responses = new HashMap<>();
	private static Object lock = new Object();
	
	public static String waitForResponse(UUID id, String script) throws InterruptedException{
		return waitForResponse(id, script, DEF_TIMEOUT_MIN);
	}
	
	public static String waitForResponse(UUID id, String script, Integer timeoutMin) throws InterruptedException{
		if (timeoutMin == null) {
			timeoutMin = DEF_TIMEOUT_MIN;
		}
		long start = System.currentTimeMillis();
		long now = System.currentTimeMillis();
		String response = null;
		while ((now - start) / 1000 / 60 < timeoutMin){
			Thread.sleep(2000);
			now = System.currentTimeMillis();
			synchronized (lock) {
				response = responses.remove(id);
			}
			if (response != null) {
				return response; 
			}
			
		}
		return "Request timed out.";
	}
	
	public static void registerResponse(UUID id, String response) {
		synchronized (lock) {
			responses.put(id, response);
		}
	}
}