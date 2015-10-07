package com.codeshelf.behavior;

interface BatchProccessor {

	int doSetup();

	int doBatch(int batchCount);

	void doTeardown();

	boolean isDone();
	
}