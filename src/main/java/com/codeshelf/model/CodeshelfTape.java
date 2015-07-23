package com.codeshelf.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Location;

import lombok.Getter;

public class CodeshelfTape {
	// format = %AABBBBBBCCCD" where AABBBBBB = mfr/guid, CCC = left offset in cm, D = reserved (0)
	final static String	TAPE_REGEX			= "%[0-9]{11}0";
	final static String	BASE32_HEADER_REGEX	= "[0Oo1IiLl23456789AaBbCcDdEeFfGgHhJjKkMmNnPpQqRrSsTtVvWwXxYyZz]{4,6}";
	private static final Logger	LOGGER			= LoggerFactory.getLogger(CodeshelfTape.class);

	@Getter
	int					guid;
	@Getter
	int					offsetCm;

	private CodeshelfTape(int guid, int offsetCm) {
		this.guid = guid;
		this.offsetCm = offsetCm;
	}

	public int getManufacturerId() {
		return guid / 1000000;
	}

	public int getManufacturerSerialNumber() {
		return guid % 1000000;
	}

	public static CodeshelfTape scan(String tapeString) {
		if (!tapeString.matches(TAPE_REGEX))
			return null;

		int g = Integer.parseInt(tapeString.substring(1, 9));
		int cm = Integer.parseInt(tapeString.substring(9, 12));
		return new CodeshelfTape(g, cm);
	}

	public static int extractGuid(String tapeString) {
		if (tapeString.matches(BASE32_HEADER_REGEX)) {
			return base32toInt(tapeString);
		}
		CodeshelfTape tape = CodeshelfTape.scan(tapeString);
		if (tape != null) {
			return tape.getGuid();
		}
		return -1;
	}

	public static int extractCmFromLeft(String tapeString) {
		CodeshelfTape tape = CodeshelfTape.scan(tapeString);
		if (tape != null) {
			return tape.getOffsetCm();
		}
		return 0;
	}

	// base32 encoding as specified by Douglas Crockford. 
	// does not use letters I L O U to avoid ambiguity and accidental obscenity
	// http://www.crockford.com/wrmg/base32.html
	final private static String	base32chars		= "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
	final private static String	base32charsAlt	= "OI23456789abcdefghjkmnpqrstvwxyz";

	private static int base32toInt(String base32string) {
		if (base32string == null)
			return -1;
		if (base32string.length() > 6)
			return -1;

		int result = 0;
		;
		int shift = 0;

		for (int i = base32string.length() - 1; i >= 0; i--) {
			int ch = base32string.charAt(i);
			int digit = base32chars.indexOf(ch);
			if (digit < 0) {
				digit = base32charsAlt.indexOf(ch);
				if (digit < 0) {
					if (ch == 'o')
						digit = 0;
					else if (ch == 'i' || ch == 'L' || ch == 'l')
						digit = 1;
					else
						return -1;
				}
			}
			result += (digit << shift);
			shift += 5;
		}
		return result;
	}

	public static String intToBase32(int value) {
		String result = "";
		while (value > 0) {
			int digit = value % 32;
			result = base32chars.substring(digit, digit + 1) + result;
			value = value >> 5;
		}
		while (result.length() < 4) {
			result = base32chars.substring(0, 1) + result;
		}
		return result;
	}

	/**
	 * Finds the location that a tapeId is associated with.
	 * There should only ever be a single location associated with a tapeId
	 * @param inFacility	The facility
	 * @param inTapeId		The tapeId to search for
	 * @return Location		Returns null if tapeId is not associated to a location
	 */
	public static Location findLocationForTapeId(int inTapeId) {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("tapeId", inTapeId));
		List<Location> locations = Location.staticGetLocationDao().findByFilter(filterParams);
		return locations.isEmpty() ? null : locations.get(0);
	}
	
	public static TapeLocation findFinestLocationForTape(String tapeScan) {
		CodeshelfTape tape = scan(tapeScan);
		TapeLocation tapeLocation = new TapeLocation();
		if (tape == null) {
			LOGGER.warn("scanned:{} value looked like tape, but did not match tape regex.", tapeScan);
			return tapeLocation;
		}
		tapeLocation.location = findFinestLocationForTapeId(tape.getGuid(), tape.getOffsetCm());
		if (tapeLocation.location != null) {
			tapeLocation.cmOffset = tapeLocation.location.isSlot() ? 0 : tape.getOffsetCm();
		}
		else {
			String valueOnTape = CodeshelfTape.intToBase32(tape.getGuid());
			LOGGER.warn("scanned:{} value on tape:{}. No location found with that tape ID.", tapeScan, valueOnTape);
		}
		return tapeLocation;
	}

	private static Location findFinestLocationForTapeId(int inTapeId, int cmOffset) {
		Location location = findLocationForTapeId(inTapeId);
		if (location == null || !location.isTier()){
			return location;
		}
		List<Location> children = location.getActiveChildren();
		boolean xOriented = location.isLocationXOriented();
		double leftOffsetSm = location.isLeftSideTowardsB1S1() ? cmOffset : location.getLocationWidthMeters() * 100 - cmOffset;
		for (Location child : children) {
			double startCm = (xOriented ? child.getAnchorPosX() : child.getAnchorPosY()) * 100;
			double endCm = startCm + child.getLocationWidthMeters() * 100;
			if (startCm <= leftOffsetSm && endCm >= leftOffsetSm){
				return child;
			}
		}
		return location;
	}
	
	public static class TapeLocation{
		@Getter
		private Location location;
		@Getter
		private int cmOffset;
	}
}
