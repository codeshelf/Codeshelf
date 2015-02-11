package com.codeshelf.event;

import java.util.Set;

import com.google.common.collect.Sets;

public enum EventTag {
	IMPORT,
	ORDER_OUTBOUND,
	ORDER_LOCATION,
	LOCATION_ALIAS,
	INVENTORY_SLOTTED,
	ORDER_CROSSBATCH, 
	LOCATION;
	
	public static Set<EventTag> tags(EventTag... inTags) {
		return Sets.newHashSet(inTags);
	}
}
