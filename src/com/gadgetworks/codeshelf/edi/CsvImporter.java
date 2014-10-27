package com.gadgetworks.codeshelf.edi;

import com.gadgetworks.codeshelf.event.EventProducer;

public abstract class CsvImporter {

	private final EventProducer	mEventProducer;

	public CsvImporter() {
		mEventProducer = new EventProducer();
	}

	protected EventProducer getEventProducer() {
		return mEventProducer;
	}
}
