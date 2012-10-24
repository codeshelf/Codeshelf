/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlProtocolEnum.java,v 1.3 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum ControlProtocolEnum {
	@EnumValue("INVALID")
	INVALID(ControlProtocolNum.INVALID, "INVALID"),
	@EnumValue("CODESHELF")
	CODESHELF(ControlProtocolNum.CODESHELF, "CODESHELF"),
	@EnumValue("ATOP")
	ATOP(ControlProtocolNum.ATOP, "ATOP");

	private int		mValue;
	private String	mName;

	ControlProtocolEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static ControlProtocolEnum geControlProtocolEnum(int inProtocolNum) {
		ControlProtocolEnum result;

		switch (inProtocolNum) {
			case ControlProtocolNum.CODESHELF:
				result = ControlProtocolEnum.CODESHELF;
				break;

			case ControlProtocolNum.ATOP:
				result = ControlProtocolEnum.ATOP;
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
		static final byte	CODESHELF	= 1;
		static final byte	ATOP		= 2;

		private ControlProtocolNum() {
		};
	}
}
