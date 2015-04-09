package com.codeshelf.edi;

import java.util.Comparator;

import com.codeshelf.model.domain.Slot;

class SlotNameComparator implements Comparator<Slot> {
	// Just order slots S1, S2, S3, etc. 
	public int compare(Slot inLoc1, Slot inLoc2) {

		if ((inLoc1 == null) && (inLoc2 == null)) {
			return 0;
		} else if (inLoc2 == null) {
			return -1;
		} else if (inLoc1 == null) {
			return 1;
		} else {
			// We need to sort S1 - S9, S10- S19, etc. Not S1, S10, S11, ... S2
			String slotOneNumerals = inLoc1.getDomainId().substring(1); // Strip off the S
			String slotTwoNumerals = inLoc2.getDomainId().substring(1); // Strip off the S
			Integer slotOneValue = Integer.valueOf(slotOneNumerals);
			Integer slotTwoValue = Integer.valueOf(slotTwoNumerals);
			return slotOneValue.compareTo(slotTwoValue);
		}
	}
}