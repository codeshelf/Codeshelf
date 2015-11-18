package com.codeshelf.model;

import lombok.Setter;

public abstract class SingleBatchProcessorABC implements BatchProcessor{
	@Setter
	private boolean done = false;

	@Override
	public int doSetup() throws Exception {
		return 1;
	}

	@Override
	public void doTeardown() {
		
	}

	@Override
	public boolean isDone() {
		return done;
	}
}
