/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandEnum.java,v 1.2 2012/02/21 02:45:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

/**
 * @author jeffw
 *
 */
public enum WebSessionCommandEnum {
	INVALID(WebSessionCommandNum.INVALID, "INVALID"),
	LAUNCH_CODE(WebSessionCommandNum.LAUNCH_CODE, "LAUNCH_CODE"),
	OBJECT_QUERY_REQ(WebSessionCommandNum.OBJECT_QUERY_REQ, "OBJECT_QUERY_REQ"),
	OBJECT_CREATE_REQ(WebSessionCommandNum.OBJECT_CREATE_REQ, "OBJECT_CREATE_REQ"),
	OBJECT_GETBYID_REQ(WebSessionCommandNum.OBJECT_GETBYID_REQ, "OBJECT_GETBYID_REQ"),
	OBJECT_CHANGE_REQ(WebSessionCommandNum.OBJECT_CHANGE_REQ, "OBJECT_CHANGE_REQ"),
	OBJECT_DELETE_REQ(WebSessionCommandNum.OBJECT_DELETE_REQ, "OBJECT_DELETE_REQ");

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

			case WebSessionCommandNum.OBJECT_QUERY_REQ:
				result = WebSessionCommandEnum.OBJECT_QUERY_REQ;
				break;

			case WebSessionCommandNum.OBJECT_GETBYID_REQ:
				result = WebSessionCommandEnum.OBJECT_GETBYID_REQ;
				break;

			case WebSessionCommandNum.OBJECT_CREATE_REQ:
				result = WebSessionCommandEnum.OBJECT_CREATE_REQ;
				break;

			case WebSessionCommandNum.OBJECT_CHANGE_REQ:
				result = WebSessionCommandEnum.OBJECT_CHANGE_REQ;
				break;

			case WebSessionCommandNum.OBJECT_DELETE_REQ:
				result = WebSessionCommandEnum.OBJECT_DELETE_REQ;
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
		static final byte	OBJECT_QUERY_REQ		= 2;
		static final byte	OBJECT_GETBYID_REQ		= 3;
		static final byte	OBJECT_CREATE_REQ	= 4;
		static final byte	OBJECT_CHANGE_REQ	= 5;
		static final byte	OBJECT_DELETE_REQ	= 6;

		private WebSessionCommandNum() {

		}
	}
}
