package com.codeshelf.security;

import lombok.Getter;

/*
 * A set of up to 8 flags represented by a byte
 */
public class SessionFlags {
	public static enum Flag {
		AUTO_REFRESH_SESSION(0, true),
		ACCOUNT_RECOVERY(1, false),
		ACCOUNT_SETUP(2, false);

		@Getter
		private byte	bit;
		@Getter
		private boolean	inverted; // true is stored as 0 ; default is true

		Flag(int bit, boolean inverted) {
			this.bit = (byte) bit;
			this.inverted = inverted;
		}
	};

	@Getter
	byte	packed;

	public SessionFlags() {
		this.packed = 0; // defaults
	}

	public SessionFlags(byte packed) {
		this.packed = packed;
	}

	public boolean get(Flag flag) {
		boolean value = (((byte)0x01 & (packed >> flag.bit)) == 1);
		return (value != flag.isInverted());
	}

	void set(Flag flag, boolean value) {
		if (value != flag.isInverted()) {
			packed |= (1 << flag.getBit()); // turn on bit
		} else {
			packed &= ((byte)0xff ^ (1 << flag.getBit())); // turn off bit
		}
	}

}
