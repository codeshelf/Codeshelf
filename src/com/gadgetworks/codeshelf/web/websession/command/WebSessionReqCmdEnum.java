/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdEnum.java,v 1.1 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

/**
 * @author jeffw
 *
 */
public enum WebSessionReqCmdEnum {
	INVALID(WebSessionReqCmdNum.INVALID, "INVALID"),
	LAUNCH_CODE_CHECK(WebSessionReqCmdNum.LAUNCH_CODE_CHECK, "LAUNCH_CODE_CHECK"),
	OBJECT_GETTER_REQ(WebSessionReqCmdNum.OBJECT_GETTER_REQ, "OBJECT_GETTER_REQ"),
	OBJECT_CREATE_REQ(WebSessionReqCmdNum.OBJECT_CREATE_REQ, "OBJECT_CREATE_REQ"),
	OBJECT_GETBYID_REQ(WebSessionReqCmdNum.OBJECT_GETBYID_REQ, "OBJECT_GETBYID_REQ"),
	OBJECT_UPDATE_REQ(WebSessionReqCmdNum.OBJECT_UPDATE_REQ, "OBJECT_UPDATE_REQ"),
	OBJECT_DELETE_REQ(WebSessionReqCmdNum.OBJECT_DELETE_REQ, "OBJECT_DELETE_REQ"),
	OBJECT_LISTENER_REQ(WebSessionReqCmdNum.OBJECT_LISTENER_REQ, "OBJECT_LISTENER_REQ");

	private int		mValue;
	private String	mName;

	WebSessionReqCmdEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WebSessionReqCmdEnum getWebSessionReqCmdEnum(int inSessionCmdNum) {
		WebSessionReqCmdEnum result;

		switch (inSessionCmdNum) {
			case WebSessionReqCmdNum.INVALID:
				result = WebSessionReqCmdEnum.INVALID;
				break;

			case WebSessionReqCmdNum.LAUNCH_CODE_CHECK:
				result = WebSessionReqCmdEnum.LAUNCH_CODE_CHECK;
				break;

			case WebSessionReqCmdNum.OBJECT_GETTER_REQ:
				result = WebSessionReqCmdEnum.OBJECT_GETTER_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_GETBYID_REQ:
				result = WebSessionReqCmdEnum.OBJECT_GETBYID_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_CREATE_REQ:
				result = WebSessionReqCmdEnum.OBJECT_CREATE_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_UPDATE_REQ:
				result = WebSessionReqCmdEnum.OBJECT_UPDATE_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_DELETE_REQ:
				result = WebSessionReqCmdEnum.OBJECT_DELETE_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_LISTENER_REQ:
				result = WebSessionReqCmdEnum.OBJECT_LISTENER_REQ;
				break;

			default:
				result = WebSessionReqCmdEnum.INVALID;
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

	final static class WebSessionReqCmdNum {
		static final byte	INVALID					= 0;
		static final byte	LAUNCH_CODE_CHECK		= 1;
		static final byte	OBJECT_GETTER_REQ		= 2;
		static final byte	OBJECT_GETBYID_REQ		= 3;
		static final byte	OBJECT_CREATE_REQ		= 4;
		static final byte	OBJECT_UPDATE_REQ		= 5;
		static final byte	OBJECT_DELETE_REQ		= 6;
		static final byte	OBJECT_LISTENER_REQ		= 7;

		private WebSessionReqCmdNum() {

		}
	}
}
