package com.gadgetworks.codeshelf.util;

public class CompareNullChecker {

	public CompareNullChecker() {}
	
	/**
	 * The idea is to provide for simple syntax to avoid cloning throughout our comparators.
	 * Should null sort first or last? We could add a parameter for this. For now, the answer is last, primarily for work instruction
	 * sorts so that the good work instructions come first.
	 * Important: if both are null, return as if the first were null. Calling code interprets a zero return as both non-null
	 */
	public static int compareNulls(Object inObject1, Object inObject2){
		if (inObject1 == null) 
			return 1;
		else if (inObject2 == null)
			return -1;
		else 
			return 0;
	}
}
