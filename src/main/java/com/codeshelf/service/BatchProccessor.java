package com.codeshelf.service;

interface BatchProccessor {

	int doSetup();

	int doBatch(int batchCount);

	void doTeardown();

	boolean isDone();
	
}