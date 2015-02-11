/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PickStrategyEnum.java,v 1.3 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum PickStrategyEnum {
	// @EnumValue("INVALID")
	INVALID(PickStrategyNum.INVALID, "INVALID"),
	// @EnumValue("SERIAL")
	SERIAL(PickStrategyNum.SERIAL, "SERIAL"),
	// @EnumValue("PARALLEL")
	PARALLEL(PickStrategyNum.PARALLEL, "PARALLEL");

	private int		mValue;
	private String	mName;

	PickStrategyEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static PickStrategyEnum getPickStrategyEnum(int inPickStrategy) {
		PickStrategyEnum result;

		switch (inPickStrategy) {
			case PickStrategyNum.SERIAL:
				result = PickStrategyEnum.SERIAL;
				break;

			case PickStrategyNum.PARALLEL:
				result = PickStrategyEnum.PARALLEL;
				break;

			default:
				result = PickStrategyEnum.INVALID;
				break;

		}

		return result;
	}

	public int getValue() {
		return mValue;
	}

	public String getName() {
		return mName;
	}

	static final class PickStrategyNum {

		static final byte	INVALID		= -1;
		static final byte	SERIAL		= 0;
		static final byte	PARALLEL	= 1;

		private PickStrategyNum() {
		};
	}
}
