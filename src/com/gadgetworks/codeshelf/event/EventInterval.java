package com.gadgetworks.codeshelf.event;

/**
 * Describe the point of time over which an event occurs.  
 * 
 * @see EventProducer#produceEventInterval(java.util.Set, Object)
 * @see EventProducer#produceEvent
 */
public enum EventInterval {
	/**
	 * Use at the beginning of an event that covers a period of time
	 */
	BEGIN,
	
	/**
	 * Use at the end of an event that covers a period of time
	 */
	END,
	
	/**
	 * Use when the event happens at an exact point of time
	 */
	INSTANTANEOUS
}