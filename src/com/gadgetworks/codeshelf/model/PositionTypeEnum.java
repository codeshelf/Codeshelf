/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PositionTypeEnum.java,v 1.2 2012/06/27 05:07:51 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, GPS=GPS, METERS_FROM_PARENT=METERS_FROM_PARENT, METERS_FROM_DATUM=METERS_FROM_DATA")
public enum PositionTypeEnum {
	INVALID(PositionTypeNum.INVALID, "INVALID"),
	GPS(PositionTypeNum.GPS, "GPS"),
	METERS_FROM_PARENT(PositionTypeNum.METERS_FROM_PARENT, "METERS_FROM_PARENT"),
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

		static final byte	INVALID				= 0;
		static final byte	GPS					= 1;
		static final byte	METERS_FROM_PARENT	= 2;
		static final byte	METERS_FROM_DATUM	= 3;

		private PositionTypeNum() {
		};
	}
}
