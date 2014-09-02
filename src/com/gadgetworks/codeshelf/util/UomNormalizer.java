package com.gadgetworks.codeshelf.util;

import com.google.common.base.Preconditions;

/**
 * Important: we need to match each pick orders to each inventory, and case to case. But the UOMs come from customer data. They may vary.
 * We need to normalize to common units. This is used in item's makeDomainId, and for finding inventory to fulfill orders.
 * 
 */
public class UomNormalizer {

	public static final String EACH = "EA";
	public static final String CASE = "CS";
	
	public UomNormalizer() {

	}

	// So far, only case and each.  In english, also may need carton, jar, box
	public static String normalizeString(String inUomStr) {
		String returnStr = inUomStr;
		
		if (inUomStr.equalsIgnoreCase("cs") || inUomStr.equalsIgnoreCase("case"))
			returnStr = CASE;
		else if (inUomStr.equalsIgnoreCase("ea") || inUomStr.equalsIgnoreCase("each"))
			returnStr = EACH;

		return returnStr;
	}

	public static boolean normalizedEquals(String inFirstUom, String inSecondUom) {
		Preconditions.checkNotNull(inFirstUom, "firstUom should not be null");
		Preconditions.checkNotNull(inSecondUom, "secondUom should not be null");
		
		return normalizeString(inFirstUom).equals(normalizeString(inSecondUom));
	}

}

