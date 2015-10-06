package com.codeshelf.api.resources.subresources;

interface BatchCallable {

	int doSetup();

	int doBatch(int batchCount);

	void doTeardown();

	boolean isDone();
	
}