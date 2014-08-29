/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PositionTypeEnum.java,v 1.5 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import javax.persistence.Enumerated;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum PositionTypeEnum {
	// @EnumValue("INVALID")
	INVALID(PositionTypeNum.INVALID, "INVALID"),
	// @EnumValue("GPS")
	GPS(PositionTypeNum.GPS, "GPS"),
	// @EnumValue("METERS_PARENT")
	METERS_FROM_PARENT(PositionTypeNum.METERS_FROM_PARENT, "METERS_FROM_PARENT"),
	// @EnumValue("METERS_DATUM")
	METERS_FROM_DATUM(PositionTypeNum.METERS_FROM_DATUM, "METERS_FROM_DATUM");

	private int		mValue;
	private String	mName;

	PositionTypeEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static PositionTypeEnum getPositionTypeEnum(int inPositionTypeID) {
		PositionTypeEnum result;

		switch (inPositionTypeID) {
			case PositionTypeNum.GPS:
				result = PositionTypeEnum.GPS;
				break;

			case PositionTypeNum.METERS_FROM_PARENT:
				result = PositionTypeEnum.METERS_FROM_PARENT;
				break;

			case PositionTypeNum.METERS_FROM_DATUM:
				result = PositionTypeEnum.METERS_FROM_DATUM;
				break;

			default:
				result = PositionTypeEnum.INVALID;
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

	final static class PositionTypeNum {

		static final byte	INVALID				= -1;
		static final byte	GPS					= 0;
		static final byte	METERS_FROM_PARENT	= 1;
		static final byte	METERS_FROM_DATUM	= 2;

		private PositionTypeNum() {
		};
	}
}
