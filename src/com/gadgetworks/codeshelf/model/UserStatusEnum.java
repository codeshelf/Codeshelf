/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UserStatusEnum.java,v 1.6 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum UserStatusEnum {
	@EnumValue("INVALID")
	INVALID(UserStatusNum.INVALID, "INVALID"),
	@EnumValue("AVAILABLE")
	AVAILABLE(UserStatusNum.AVAILABLE, "AVAILABLE"),
	@EnumValue("AWAY")
	AWAY(UserStatusNum.AWAY, "AWAY"),
	@EnumValue("CHAT")
	CHAT(UserStatusNum.CHAT, "CHAT"),
	@EnumValue("DND")
	DND(UserStatusNum.DND, "DND"),
	@EnumValue("XA")
	XA(UserStatusNum.XA, "XA");

	private int		mValue;
	private String	mName;

	UserStatusEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static UserStatusEnum getUserStatusEnum(int inQueryTypeID) {
		UserStatusEnum result;

		switch (inQueryTypeID) {
			case UserStatusNum.AVAILABLE:
				result = UserStatusEnum.AVAILABLE;
				break;

			case UserStatusNum.AWAY:
				result = UserStatusEnum.AWAY;
				break;

			case UserStatusNum.CHAT:
				result = UserStatusEnum.CHAT;
				break;

			case UserStatusNum.DND:
				result = UserStatusEnum.DND;
				break;

			case UserStatusNum.XA:
				result = UserStatusEnum.XA;
				break;

			default:
				result = UserStatusEnum.INVALID;
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

	static final class UserStatusNum {

		static final byte	INVALID		= -1;
		static final byte	AVAILABLE	= 0;
		static final byte	AWAY		= 1;
		static final byte	CHAT		= 2;
		static final byte	DND			= 3;
		static final byte	XA			= 4;

		private UserStatusNum() {
		};
	}
}
