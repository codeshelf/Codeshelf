/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: EffectEnum.java,v 1.2 2013/05/28 05:14:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.command;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum EffectEnum {

	INVALID(EffectNum.INVALID, "INVALID"),
	SOLID(EffectNum.SOLID, "SOLID"),
	FLASH(EffectNum.FLASH, "FLASH"),
	ERROR(EffectNum.ERROR, "ERROR"),
	MOTEL(EffectNum.MOTEL, "MOTEL");

	private byte	mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	EffectEnum(final byte inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inMotorControlCommandID
	 *  @return
	 */
	public static EffectEnum getEffectEnum(int inEffectNum) {
		EffectEnum result = EffectEnum.INVALID;

		switch (inEffectNum) {
			case EffectNum.SOLID:
				result = EffectEnum.SOLID;
				break;
			case EffectNum.FLASH:
				result = EffectEnum.FLASH;
				break;
			case EffectNum.ERROR:
				result = EffectEnum.ERROR;
				break;
			case EffectNum.MOTEL:
				result = EffectEnum.MOTEL;
				break;
			default:
				break;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getValue() {
		return mValue;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getName() {
		return mName;
	}

	final static class EffectNum {
		static final byte	INVALID	= -1;
		static final byte	SOLID	= 0;
		static final byte	FLASH	= 1;
		static final byte	ERROR	= 2;
		static final byte	MOTEL	= 3;

		private EffectNum() {

		};
	}
}
