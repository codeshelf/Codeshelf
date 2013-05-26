/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: EffectEnum.java,v 1.1 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.command;

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

	private int		mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	EffectEnum(final int inCmdValue, final String inName) {
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
	public int getValue() {
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
		static final int	INVALID	= -1;
		static final int	SOLID	= 0;
		static final int	FLASH	= 1;
		static final int	ERROR	= 2;
		static final int	MOTEL	= 3;

		private EffectNum() {

		};
	}
}
