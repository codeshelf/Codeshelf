package com.codeshelf.model;

import lombok.Getter;

public class CodeshelfTape {
	// format = %AABBBBBBCCCD" where AABBBBBB = mfr/guid, CCC = left offset in cm, D = reserved (0)
	final static String	TAPE_REGEX			= "%[0-9]{11}0";
	final static String	BASE32_HEADER_REGEX	= "[0Oo1IiLl23456789AaBbCcDdEeFfGgHhJjKkMmNnPpQqRrSsTtVvWwXxYyZz]{4,6}";

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

}
