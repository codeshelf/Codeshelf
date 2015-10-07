package com.codeshelf.behavior;

public interface BatchProcessor {

	int doSetup();

	int doBatch(int batchCount);

	int doTeardown();

	boolean isDone();
	
	public String toString();
	
}