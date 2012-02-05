/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandEnum.java,v 1.1 2012/02/05 08:41:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;


/**
 * @author jeffw
 *
 */
public enum WebSessionCommandEnum {
	INVALID(WebSessionCommandNum.INVALID, "INVALID"),
	LAUNCH_CODE(WebSessionCommandNum.LAUNCH_CODE, "LAUNCH_CODE");

	private int		mValue;
	private String	mName;

	WebSessionCommandEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WebSessionCommandEnum getWebSessionCommandEnum(int inSessionCommandNumber) {
		WebSessionCommandEnum result;

		switch (inSessionCommandNumber) {
			case WebSessionCommandNum.INVALID:
				result = WebSessionCommandEnum.INVALID;
				break;

			case WebSessionCommandNum.LAUNCH_CODE:
				result = WebSessionCommandEnum.LAUNCH_CODE;
				break;

			default:
				result = WebSessionCommandEnum.INVALID;
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

	final static class WebSessionCommandNum {
		static final byte	INVALID			= 0;
		static final byte	LAUNCH_CODE		= 1;

		private WebSessionCommandNum() {

		}
	}
}
