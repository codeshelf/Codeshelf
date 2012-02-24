/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCmdEnum.java,v 1.1 2012/02/24 07:41:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

/**
 * @author jeffw
 *
 */
public enum WebSessionCmdEnum {
	INVALID(WebSessionCmdNum.INVALID, "INVALID"),
	LAUNCH_CODE_CHECK(WebSessionCmdNum.LAUNCH_CODE_CHECK, "LAUNCH_CODE_CHECK"),
	LAUNCH_CODE_RESP(WebSessionCmdNum.LAUNCH_CODE_RESP, "LAUNCH_CODE_RESP"),
	OBJECT_GETTER_REQ(WebSessionCmdNum.OBJECT_GETTER_REQ, "OBJECT_GETTER_REQ"),
	OBJECT_GETTER_RESP(WebSessionCmdNum.OBJECT_GETTER_RESP, "OBJECT_GETTER_RESP"),
	OBJECT_CREATE_REQ(WebSessionCmdNum.OBJECT_CREATE_REQ, "OBJECT_CREATE_REQ"),
	OBJECT_CREATE_RESP(WebSessionCmdNum.OBJECT_CREATE_RESP, "OBJECT_CREATE_RESP"),
	OBJECT_GETBYID_REQ(WebSessionCmdNum.OBJECT_GETBYID_REQ, "OBJECT_GETBYID_REQ"),
	OBJECT_GETBYID_RESP(WebSessionCmdNum.OBJECT_GETBYID_RESP, "OBJECT_GETBYID_RESP"),
	OBJECT_UPDATE_REQ(WebSessionCmdNum.OBJECT_UPDATE_REQ, "OBJECT_UPDATE_REQ"),
	OBJECT_UPDATE_RESP(WebSessionCmdNum.OBJECT_UPDATE_RESP, "OBJECT_UPDATE_RESP"),
	OBJECT_DELETE_REQ(WebSessionCmdNum.OBJECT_DELETE_REQ, "OBJECT_DELETE_REQ"),
	OBJECT_DELETE_RESP(WebSessionCmdNum.OBJECT_DELETE_RESP, "OBJECT_DELETE_RESP");

	private int		mValue;
	private String	mName;

	WebSessionCmdEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WebSessionCmdEnum getWebSessionCmdEnum(int inSessionCmdNum) {
		WebSessionCmdEnum result;

		switch (inSessionCmdNum) {
			case WebSessionCmdNum.INVALID:
				result = WebSessionCmdEnum.INVALID;
				break;

			case WebSessionCmdNum.LAUNCH_CODE_CHECK:
				result = WebSessionCmdEnum.LAUNCH_CODE_CHECK;
				break;

			case WebSessionCmdNum.LAUNCH_CODE_RESP:
				result = WebSessionCmdEnum.LAUNCH_CODE_RESP;
				break;

			case WebSessionCmdNum.OBJECT_GETTER_REQ:
				result = WebSessionCmdEnum.OBJECT_GETTER_REQ;
				break;

			case WebSessionCmdNum.OBJECT_GETTER_RESP:
				result = WebSessionCmdEnum.OBJECT_GETTER_RESP;
				break;

			case WebSessionCmdNum.OBJECT_GETBYID_REQ:
				result = WebSessionCmdEnum.OBJECT_GETBYID_REQ;
				break;

			case WebSessionCmdNum.OBJECT_GETBYID_RESP:
				result = WebSessionCmdEnum.OBJECT_GETBYID_RESP;
				break;

			case WebSessionCmdNum.OBJECT_CREATE_REQ:
				result = WebSessionCmdEnum.OBJECT_CREATE_REQ;
				break;

			case WebSessionCmdNum.OBJECT_CREATE_RESP:
				result = WebSessionCmdEnum.OBJECT_CREATE_RESP;
				break;

			case WebSessionCmdNum.OBJECT_UPDATE_REQ:
				result = WebSessionCmdEnum.OBJECT_UPDATE_REQ;
				break;

			case WebSessionCmdNum.OBJECT_UPDATE_RESP:
				result = WebSessionCmdEnum.OBJECT_UPDATE_RESP;
				break;

			case WebSessionCmdNum.OBJECT_DELETE_REQ:
				result = WebSessionCmdEnum.OBJECT_DELETE_REQ;
				break;

			case WebSessionCmdNum.OBJECT_DELETE_RESP:
				result = WebSessionCmdEnum.OBJECT_DELETE_RESP;
				break;

			default:
				result = WebSessionCmdEnum.INVALID;
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

	final static class WebSessionCmdNum {
		static final byte	INVALID					= 0;
		static final byte	LAUNCH_CODE_CHECK				= 1;
		static final byte	LAUNCH_CODE_RESP		= 2;
		static final byte	OBJECT_GETTER_REQ	= 3;
		static final byte	OBJECT_GETTER_RESP	= 4;
		static final byte	OBJECT_GETBYID_REQ		= 5;
		static final byte	OBJECT_GETBYID_RESP		= 6;
		static final byte	OBJECT_CREATE_REQ		= 7;
		static final byte	OBJECT_CREATE_RESP		= 8;
		static final byte	OBJECT_UPDATE_REQ		= 9;
		static final byte	OBJECT_UPDATE_RESP		= 10;
		static final byte	OBJECT_DELETE_REQ		= 11;
		static final byte	OBJECT_DELETE_RESP		= 12;

		private WebSessionCmdNum() {

		}
	}
}
