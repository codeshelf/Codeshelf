/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: TagProtocolEnum.java,v 1.3 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum TagProtocolEnum {
	@EnumValue("INVALID")
	INVALID(TagProtocolNum.INVALID, "INVALID"),
	@EnumValue("ATOP")
	ATOP(TagProtocolNum.ATOP, "ATOP");

	private int		mValue;
	private String	mName;

	TagProtocolEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static TagProtocolEnum getTagProtocolEnum(int inSourceTypeID) {
		TagProtocolEnum result;

		switch (inSourceTypeID) {
			case TagProtocolNum.ATOP:
				result = TagProtocolEnum.ATOP;
				break;

			default:
				result = TagProtocolEnum.INVALID;
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

	final static class TagProtocolNum {

		static final byte	INVALID	= 0;
		static final byte	ATOP	= 1;

		private TagProtocolNum() {
		};
	}
}
