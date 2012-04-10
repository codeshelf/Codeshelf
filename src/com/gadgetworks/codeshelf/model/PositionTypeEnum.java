/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PositionTypeEnum.java,v 1.1 2012/04/10 08:01:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, GPS=GPS, METERS=METERS")
public enum PositionTypeEnum {
	INVALID(PositionTypeNum.INVALID, "INVALID"),
	GPS(PositionTypeNum.GPS, "GPS"),
	METERS(PositionTypeNum.METERS, "METERS");

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

			case PositionTypeNum.METERS:
				result = PositionTypeEnum.METERS;
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

		static final byte	INVALID	= 0;
		static final byte	GPS		= 1;
		static final byte	METERS	= 2;

		private PositionTypeNum() {
		};
	}
}
