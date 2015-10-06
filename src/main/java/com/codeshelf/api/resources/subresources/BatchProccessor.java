package com.codeshelf.api.resources.subresources;

interface BatchProccessor {

	int doSetup();

	int doBatch(int batchCount);

	void doTeardown();

	boolean isDone();
	
}