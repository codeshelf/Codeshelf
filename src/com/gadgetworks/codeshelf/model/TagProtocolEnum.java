/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: TagProtocolEnum.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, ATOP=ATOP")
public enum TagProtocolEnum {
	INVALID(TagProtocolNum.INVALID, "INVALID"),
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
