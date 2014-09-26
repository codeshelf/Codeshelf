package com.gadgetworks.codeshelf.util;

public class SequenceNumber {

	static long lastTime=0;
	static int counter=0;
	
	final public static synchronized long generate() {
		long seq=0;
		long currentTime = System.currentTimeMillis();
		if (currentTime==lastTime) {
			counter++;
			if (counter>=100) {
				while (currentTime==lastTime) {
					currentTime = System.currentTimeMillis();
				}
			}
		}
		if (currentTime!=lastTime) {
			counter=0;
			lastTime = currentTime;
		}
		seq = currentTime*100+counter;
		return seq;
	}
}
