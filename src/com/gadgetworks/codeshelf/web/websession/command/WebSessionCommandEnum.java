/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandEnum.java,v 1.4 2012/02/21 23:32:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

/**
 * @author jeffw
 *
 */
public enum WebSessionCommandEnum {
	INVALID(WebSessionCommandNum.INVALID, "INVALID"),
	LAUNCH_CODE(WebSessionCommandNum.LAUNCH_CODE, "LAUNCH_CODE"),
	LAUNCH_CODE_RESP(WebSessionCommandNum.LAUNCH_CODE_RESP, "LAUNCH_CODE_RESP"),
	OBJECT_QUERY_REQ(WebSessionCommandNum.OBJECT_QUERY_REQ, "OBJECT_QUERY_REQ"),
	OBJECT_QUERY_RESP(WebSessionCommandNum.OBJECT_QUERY_RESP, "OBJECT_QUERY_RESP"),
	OBJECT_CREATE_REQ(WebSessionCommandNum.OBJECT_CREATE_REQ, "OBJECT_CREATE_REQ"),
	OBJECT_CREATE_RESP(WebSessionCommandNum.OBJECT_CREATE_RESP, "OBJECT_CREATE_RESP"),
	OBJECT_GETBYID_REQ(WebSessionCommandNum.OBJECT_GETBYID_REQ, "OBJECT_GETBYID_REQ"),
	OBJECT_GETBYID_RESP(WebSessionCommandNum.OBJECT_GETBYID_RESP, "OBJECT_GETBYID_RESP"),
	OBJECT_UPDATE_REQ(WebSessionCommandNum.OBJECT_UPDATE_REQ, "OBJECT_UPDATE_REQ"),
	OBJECT_UPDATE_RESP(WebSessionCommandNum.OBJECT_UPDATE_RESP, "OBJECT_UPDATE_RESP"),
	OBJECT_DELETE_REQ(WebSessionCommandNum.OBJECT_DELETE_REQ, "OBJECT_DELETE_REQ"),
	OBJECT_DELETE_RESP(WebSessionCommandNum.OBJECT_DELETE_RESP, "OBJECT_DELETE_RESP");

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

			case WebSessionCommandNum.LAUNCH_CODE_RESP:
				result = WebSessionCommandEnum.LAUNCH_CODE_RESP;
				break;

			case WebSessionCommandNum.OBJECT_QUERY_REQ:
				result = WebSessionCommandEnum.OBJECT_QUERY_REQ;
				break;

			case WebSessionCommandNum.OBJECT_QUERY_RESP:
				result = WebSessionCommandEnum.OBJECT_QUERY_RESP;
				break;

			case WebSessionCommandNum.OBJECT_GETBYID_REQ:
				result = WebSessionCommandEnum.OBJECT_GETBYID_REQ;
				break;

			case WebSessionCommandNum.OBJECT_GETBYID_RESP:
				result = WebSessionCommandEnum.OBJECT_GETBYID_RESP;
				break;

			case WebSessionCommandNum.OBJECT_CREATE_REQ:
				result = WebSessionCommandEnum.OBJECT_CREATE_REQ;
				break;

			case WebSessionCommandNum.OBJECT_CREATE_RESP:
				result = WebSessionCommandEnum.OBJECT_CREATE_RESP;
				break;

			case WebSessionCommandNum.OBJECT_UPDATE_REQ:
				result = WebSessionCommandEnum.OBJECT_UPDATE_REQ;
				break;

			case WebSessionCommandNum.OBJECT_UPDATE_RESP:
				result = WebSessionCommandEnum.OBJECT_UPDATE_RESP;
				break;

			case WebSessionCommandNum.OBJECT_DELETE_REQ:
				result = WebSessionCommandEnum.OBJECT_DELETE_REQ;
				break;

			case WebSessionCommandNum.OBJECT_DELETE_RESP:
				result = WebSessionCommandEnum.OBJECT_DELETE_RESP;
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
		static final byte	INVALID				= 0;
		static final byte	LAUNCH_CODE			= 1;
		static final byte	LAUNCH_CODE_RESP	= 2;
		static final byte	OBJECT_QUERY_REQ	= 3;
		static final byte	OBJECT_QUERY_RESP	= 4;
		static final byte	OBJECT_GETBYID_REQ	= 5;
		static final byte	OBJECT_GETBYID_RESP	= 6;
		static final byte	OBJECT_CREATE_REQ	= 7;
		static final byte	OBJECT_CREATE_RESP	= 8;
		static final byte	OBJECT_UPDATE_REQ	= 9;
		static final byte	OBJECT_UPDATE_RESP	= 10;
		static final byte	OBJECT_DELETE_REQ	= 11;
		static final byte	OBJECT_DELETE_RESP	= 12;

		private WebSessionCommandNum() {

		}
	}
}
