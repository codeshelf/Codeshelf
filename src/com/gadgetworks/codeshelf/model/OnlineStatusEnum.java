/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: OnlineStatusEnum.java,v 1.1 2011/01/21 01:08:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, OFFLINE=OFFLINE, CONNECTING=CONNECTING, FAILED=FAILED, ONLINE=ONLINE")
public enum OnlineStatusEnum {
	INVALID(OnlineStatusNum.INVALID, "INVALID"),
	OFFLINE(OnlineStatusNum.OFFLINE, "OFFLINE"),
	CONNECTING(OnlineStatusNum.CONNECTING, "CONNECTING"),
	FAILED(OnlineStatusNum.FAILED, "FAILED"),
	ONLINE(OnlineStatusNum.ONLINE, "ONLINE");

	private int		mValue;
	private String	mName;

	OnlineStatusEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static OnlineStatusEnum getOnlineStatusEnum(int inOnlineStatusID) {
		OnlineStatusEnum result;

		switch (inOnlineStatusID) {
			case OnlineStatusNum.OFFLINE:
				result = OnlineStatusEnum.OFFLINE;
				break;

			case OnlineStatusNum.CONNECTING:
				result = OnlineStatusEnum.CONNECTING;
				break;

			case OnlineStatusNum.FAILED:
				result = OnlineStatusEnum.FAILED;
				break;

			case OnlineStatusNum.ONLINE:
				result = OnlineStatusEnum.ONLINE;
				break;

			default:
				result = OnlineStatusEnum.INVALID;
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

	static final class OnlineStatusNum {

		static final byte	INVALID		= 0;
		static final byte	OFFLINE		= 1;
		static final byte	CONNECTING	= 2;
		static final byte	FAILED		= 3;
		static final byte	ONLINE		= 4;

		private OnlineStatusNum() {
		};
	}
}
