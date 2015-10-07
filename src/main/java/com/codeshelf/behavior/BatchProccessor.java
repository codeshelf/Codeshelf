package com.codeshelf.behavior;

interface BatchProccessor {

	int doSetup();

	int doBatch(int batchCount);

	int doTeardown();

	boolean isDone();
	
}