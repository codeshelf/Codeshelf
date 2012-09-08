/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UserStatusEnum.java,v 1.4 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, AVAILABLE=AVAILABLE, AWAY=AWAY, CHAT=CHAT, DND=DND, XA=XA")
public enum UserStatusEnum {
	INVALID(UserStatusNum.INVALID, "INVALID"),
	AVAILABLE(UserStatusNum.AVAILABLE, "AVAILABLE"),
	AWAY(UserStatusNum.AWAY, "AWAY"),
	CHAT(UserStatusNum.CHAT, "CHAT"),
	DND(UserStatusNum.DND, "DND"),
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

		static final byte	INVALID		= 0;
		static final byte	AVAILABLE	= 1;
		static final byte	AWAY		= 2;
		static final byte	CHAT		= 3;
		static final byte	DND			= 4;
		static final byte	XA			= 5;

		private UserStatusNum() {
		};
	}
}
