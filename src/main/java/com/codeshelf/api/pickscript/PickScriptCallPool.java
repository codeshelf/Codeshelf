package com.codeshelf.api.pickscript;

import java.util.HashMap;
import java.util.UUID;

import com.codeshelf.ws.protocol.message.PickScriptMessage;

public class PickScriptCallPool {
	private static final int DEF_TIMEOUT_MIN = 5;
	private static HashMap<UUID, PickScriptMessage> responses = new HashMap<>();
	private static Object lock = new Object();
	
	public static PickScriptMessage waitForSiteResponse(UUID id) throws InterruptedException{
		return waitForSiteResponse(id, DEF_TIMEOUT_MIN);
	}
	
	public static PickScriptMessage waitForSiteResponse(UUID id, Integer timeoutMin) throws InterruptedException{
		if (timeoutMin == null) {
			timeoutMin = DEF_TIMEOUT_MIN;
		}
		long start = System.currentTimeMillis();
		long now = System.currentTimeMillis();
		PickScriptMessage responseMessage = null;
		while ((now - start) / 1000 / 60 < timeoutMin){
			Thread.sleep(2000);
			now = System.currentTimeMillis();
			synchronized (lock) {
				responseMessage = responses.remove(id);
			}
			if (responseMessage != null) {
				return responseMessage; 
			}
			
		}
		return null;
	}
	
	public static void registerSiteResponse(PickScriptMessage message) {
		synchronized (lock) {
			responses.put(message.getId(), message);
		}
	}	
}