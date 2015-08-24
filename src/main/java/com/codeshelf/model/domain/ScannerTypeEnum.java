package com.codeshelf.model.domain;

public enum ScannerTypeEnum {
	INVALID((byte) -1, "INVALID"),
	ORIGINALSERIAL((byte) 0, "ORIGINALSERIAL"),
	CODECORPS3600((byte) 1, "CODECORPS3600");
	// Do not change these strings. You may add new enums.
	// If you do a change a string, you would also have to do an upgrade action as some CHE in various DBs might have the old string value.
	
	private byte	mValue;
	private String	mName;

	private ScannerTypeEnum(final byte inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	public byte getValue() {
		return mValue;
	}

	public String getName() {
		return mName;
	}

	static public ScannerTypeEnum byteToScannerType(byte inByte) {
		for (ScannerTypeEnum scannerType : ScannerTypeEnum.values()) {
			if (scannerType.getValue() == inByte)
				return scannerType;
		}
		return INVALID;
	}

	/**
	 * Declared the field on CHE as nullable to make upgrade simpler. So deal with null. We want to default to original.
	 */
	static public byte scannerTypeToByte(ScannerTypeEnum inScannerType) {
		if (inScannerType == null)
			return ORIGINALSERIAL.getValue();
		else
			return inScannerType.getValue();
	}
}
