package com.codeshelf.model;

import java.util.Comparator;

import com.codeshelf.model.domain.Tier;

public class TierBayComparable implements Comparator<Tier> {
	// For the tierRight and tierLeft aisle types. 

	public int compare(Tier inLoc1, Tier inLoc2) {

		if ((inLoc1 == null) && (inLoc2 == null)) {
			return 0;
		} else if (inLoc2 == null) {
			return -1;
		} else if (inLoc1 == null) {
			return 1;
		} else {
			return inLoc1.getAisleTierBayForComparable().compareTo(inLoc2.getAisleTierBayForComparable());
		}
	}
}


