package com.codeshelf.util;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Important: we need to match each pick orders to each inventory, and case to case. But the UOMs come from customer data. They may vary.
 * We need to normalize to common units. This is used in item's makeDomainId, and for finding inventory to fulfill orders.
 * 
 */
public class UomNormalizer {

	// Once set, these shall never change as they are in the database as part of item domainId.
	public static final String EACH = "EA";
	public static final String CASE = "CS";

	private static final Map<String, List<String>> variants = ImmutableMap.<String, List<String>>of(
		EACH, ImmutableList.of("EA", "EACH", "PK", "PICK"),// PICK is sort of a kludge for Accu-Logistics
		CASE, ImmutableList.of("CS", "CASE")
	);
	
	public UomNormalizer() {

	}

	// So far, only case and each.  In english, also may need carton, jar, box, pack or package. Notice potential conflict with "pick".
	public static String normalizeString(String inUomStr) {
		String returnStr = inUomStr.toUpperCase();
		for (Map.Entry<String, List<String>>  variantEntry : variants.entrySet()) {
			List<String> variants = variantEntry.getValue();
			if (variants.contains(returnStr)) {
				return variantEntry.getKey();
			}
		}
		return returnStr;
	}

	public static boolean normalizedEquals(String inFirstUom, String inSecondUom) {
		Preconditions.checkNotNull(inFirstUom, "firstUom should not be null");
		Preconditions.checkNotNull(inSecondUom, "secondUom should not be null");
		
		return normalizeString(inFirstUom).equals(normalizeString(inSecondUom));
	}
	
	/*
	 * Separate function because of our business logic importance for each. Only one each item per facility (or later, per work area.)
	 */
	public static boolean isEach(String inUom){
		return normalizedEquals(EACH, inUom);
	}
	
	public static boolean isCase(String inUom){
		return normalizedEquals(CASE, inUom);
	}

	public static List<String> variants(String normalizedString) {
		return variants.get(normalizedString);
	}

}

