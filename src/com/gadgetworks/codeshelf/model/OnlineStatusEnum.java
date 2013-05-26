/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OnlineStatusEnum.java,v 1.5 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum OnlineStatusEnum {
	@EnumValue("INVALID")
	INVALID(OnlineStatusNum.INVALID, "INVALID"),
	@EnumValue("OFFLINE")
	OFFLINE(OnlineStatusNum.OFFLINE, "OFFLINE"),
	@EnumValue("CONNECTING")
	CONNECTING(OnlineStatusNum.CONNECTING, "CONNECTING"),
	@EnumValue("FAILED")
	FAILED(OnlineStatusNum.FAILED, "FAILED"),
	@EnumValue("ONLINE")
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

		static final byte	INVALID		= -1;
		static final byte	OFFLINE		= 0;
		static final byte	CONNECTING	= 1;
		static final byte	FAILED		= 2;
		static final byte	ONLINE		= 3;

		private OnlineStatusNum() {
		};
	}
}
