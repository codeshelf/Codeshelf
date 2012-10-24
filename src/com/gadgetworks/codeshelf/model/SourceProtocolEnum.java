/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SourceProtocolEnum.java,v 1.4 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum SourceProtocolEnum {
	@EnumValue("INVALID")
	INVALID(SourceProtocolNum.INVALID, "INVALID"),
	@EnumValue("JMS")
	JMS(SourceProtocolNum.JMS, "JMS");

	private int		mValue;
	private String	mName;

	SourceProtocolEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static SourceProtocolEnum getSourceTypeEnum(int inSourceTypeID) {
		SourceProtocolEnum result;

		switch (inSourceTypeID) {
			case SourceProtocolNum.JMS:
				result = SourceProtocolEnum.JMS;
				break;

			default:
				result = SourceProtocolEnum.INVALID;
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

	final static class SourceProtocolNum {

		static final byte	INVALID		= 0;
		static final byte	JMS			= 1;

		private SourceProtocolNum() {
		};
	}
}
