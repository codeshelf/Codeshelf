package com.codeshelf.model;

import java.util.Comparator;

import com.codeshelf.model.domain.Bay;

public class BayComparable implements Comparator<Bay> {
	// We want B1, B2, ...B9, B10,B11, etc.
	public int compare(Bay inLoc1, Bay inLoc2) {

		if ((inLoc1 == null) && (inLoc2 == null)) {
			return 0;
		} else if (inLoc2 == null) {
			return -1;
		} else if (inLoc1 == null) {
			return 1;
		} else {
			return inLoc1.getBayIdForComparable().compareTo(inLoc2.getBayIdForComparable());
		}
	}
}