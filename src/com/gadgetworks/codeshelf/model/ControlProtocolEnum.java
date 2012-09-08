/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlProtocolEnum.java,v 1.2 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, INDUSTRO=INDUSTRO, ATOP=ATOP")
public enum ControlProtocolEnum {
	INVALID(ControlProtocolNum.INVALID, "INVALID"),
	INDUSTRO(ControlProtocolNum.INDUSTRO, "INDUSTRO"),
	ATOP(ControlProtocolNum.ATOP, "ATOP"),
	RTS(ControlProtocolNum.RTS, "RTS");

	private int		mValue;
	private String	mName;

	ControlProtocolEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static ControlProtocolEnum geControlProtocolEnum(int inProtocolNum) {
		ControlProtocolEnum result;

		switch (inProtocolNum) {
			case ControlProtocolNum.INDUSTRO:
				result = ControlProtocolEnum.INDUSTRO;
				break;

			case ControlProtocolNum.ATOP:
				result = ControlProtocolEnum.ATOP;
				break;

			case ControlProtocolNum.RTS:
				result = ControlProtocolEnum.RTS;
				break;

			default:
				result = ControlProtocolEnum.INVALID;
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

	final static class ControlProtocolNum {

		static final byte	INVALID		= 0;
		static final byte	INDUSTRO	= 1;
		static final byte	ATOP		= 2;
		static final byte	RTS			= 3;

		private ControlProtocolNum() {
		};
	}
}
