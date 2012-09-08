/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionStateEnum.java,v 1.2 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

/**
 * @author jeffw
 *
 */
public enum WebSessionStateEnum {
	INVALID(WebSessionStateNum.INVALID, "INVALID"),
	ANONYMOUS(WebSessionStateNum.ANONYMOUS, "ANONYMOUS"),
	AUTHENTICATED(WebSessionStateNum.AUTHENTICATED, "AUTHENTICATED"),
	TERMINATED(WebSessionStateNum.TERMINATED, "TERMINATED");

	private int		mValue;
	private String	mName;

	WebSessionStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WebSessionStateEnum getWebSessionStateEnum(int inQueryTypeID) {
		WebSessionStateEnum result;

		switch (inQueryTypeID) {
			case WebSessionStateNum.INVALID:
				result = WebSessionStateEnum.INVALID;
				break;

			case WebSessionStateNum.ANONYMOUS:
				result = WebSessionStateEnum.ANONYMOUS;
				break;

			case WebSessionStateNum.AUTHENTICATED:
				result = WebSessionStateEnum.AUTHENTICATED;
				break;

			case WebSessionStateNum.TERMINATED:
				result = WebSessionStateEnum.TERMINATED;
				break;

			default:
				result = WebSessionStateEnum.INVALID;
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

	final static class WebSessionStateNum {
		static final byte	INVALID			= 0;
		static final byte	ANONYMOUS		= 1;
		static final byte	AUTHENTICATED	= 2;
		static final byte	TERMINATED		= 3;

		private WebSessionStateNum() {

		}
	}
}