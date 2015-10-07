package com.codeshelf.behavior;

public interface BatchProcessor {

	/** Return a total for a sense of progress */ 
	int doSetup() throws Exception;

	/** Return the latest progress (monotonically increasing) */ 
	int doBatch(int batchCount) throws Exception;

	/** called upon exception or completion */
	void doTeardown();

	/** set to true to indicate no more batches */
	boolean isDone();
	
	public String toString();
	
}