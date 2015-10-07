package com.codeshelf.behavior;

public interface BatchProcessor {

	int doSetup() throws Exception;

	int doBatch(int batchCount) throws Exception;

	void doTeardown();

	boolean isDone();
	
	public String toString();
	
}