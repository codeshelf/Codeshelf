/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdEnum.java,v 1.2 2012/03/24 06:49:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

/**
 * @author jeffw
 *
 */
public enum WebSessionRespCmdEnum {
	INVALID(WebSessionRespCmdNum.INVALID, "INVALID"),
	LAUNCH_CODE_RESP(WebSessionRespCmdNum.LAUNCH_CODE_RESP, "LAUNCH_CODE_RESP"),
	OBJECT_GETTER_RESP(WebSessionRespCmdNum.OBJECT_GETTER_RESP, "OBJECT_GETTER_RESP"),
	OBJECT_GETBYID_RESP(WebSessionRespCmdNum.OBJECT_GETBYID_RESP, "OBJECT_GETBYID_RESP"),
	OBJECT_CREATE_RESP(WebSessionRespCmdNum.OBJECT_CREATE_RESP, "OBJECT_CREATE_RESP"),
	OBJECT_UPDATE_RESP(WebSessionRespCmdNum.OBJECT_UPDATE_RESP, "OBJECT_UPDATE_RESP"),
	OBJECT_DELETE_RESP(WebSessionRespCmdNum.OBJECT_DELETE_RESP, "OBJECT_DELETE_RESP"),
	OBJECT_LISTENER_RESP(WebSessionRespCmdNum.OBJECT_LISTENER_RESP, "OBJECT_LISTENER_RESP"),
	OBJECT_FILTER_RESP(WebSessionRespCmdNum.OBJECT_LISTENER_RESP, "OBJECT_FILTER_RESP");

	private int		mValue;
	private String	mName;

	WebSessionRespCmdEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WebSessionRespCmdEnum getWebSessionRespCmdEnum(int inSessionCmdNum) {
		WebSessionRespCmdEnum result;

		switch (inSessionCmdNum) {
			case WebSessionRespCmdNum.INVALID:
				result = WebSessionRespCmdEnum.INVALID;
				break;

			case WebSessionRespCmdNum.LAUNCH_CODE_RESP:
				result = WebSessionRespCmdEnum.LAUNCH_CODE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_GETTER_RESP:
				result = WebSessionRespCmdEnum.OBJECT_GETTER_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_GETBYID_RESP:
				result = WebSessionRespCmdEnum.OBJECT_GETBYID_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_CREATE_RESP:
				result = WebSessionRespCmdEnum.OBJECT_CREATE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_UPDATE_RESP:
				result = WebSessionRespCmdEnum.OBJECT_UPDATE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_DELETE_RESP:
				result = WebSessionRespCmdEnum.OBJECT_DELETE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_LISTENER_RESP:
				result = WebSessionRespCmdEnum.OBJECT_LISTENER_RESP;
				break;

			default:
				result = WebSessionRespCmdEnum.INVALID;
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

	final static class WebSessionRespCmdNum {
		static final byte	INVALID					= 0;
		static final byte	LAUNCH_CODE_RESP		= 1;
		static final byte	OBJECT_GETTER_RESP		= 2;
		static final byte	OBJECT_GETBYID_RESP		= 3;
		static final byte	OBJECT_CREATE_RESP		= 4;
		static final byte	OBJECT_UPDATE_RESP		= 5;
		static final byte	OBJECT_DELETE_RESP		= 6;
		static final byte	OBJECT_LISTENER_RESP	= 7;

		private WebSessionRespCmdNum() {

		}
	}
}
