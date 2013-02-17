/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdEnum.java,v 1.9 2013/02/17 04:22:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

/**
 * @author jeffw
 *
 */
public enum WebSessionReqCmdEnum {
	INVALID(WebSessionReqCmdNum.INVALID, "INVALID"),
	LOGIN_REQ(WebSessionReqCmdNum.LOGIN_REQ, "LOGIN_RQ"),
	OBJECT_GETTER_REQ(WebSessionReqCmdNum.OBJECT_GETTER_REQ, "OBJ_GET_RQ"),
	OBJECT_UPDATE_REQ(WebSessionReqCmdNum.OBJECT_UPDATE_REQ, "OBJ_UPD_RQ"),
	OBJECT_DELETE_REQ(WebSessionReqCmdNum.OBJECT_DELETE_REQ, "OBJ_DEL_RQ"),
	OBJECT_LISTENER_REQ(WebSessionReqCmdNum.OBJECT_LISTENER_REQ, "OBJ_LSN_RQ"),
	OBJECT_FILTER_REQ(WebSessionReqCmdNum.OBJECT_FILTER_REQ, "OBJ_FLT_RQ"),
	OBJECT_METHOD_REQ(WebSessionReqCmdNum.OBJECT_METHOD_REQ, "OBJ_METH_RQ"),
	REGISTER_EDI_SERVICE_REQ(WebSessionReqCmdNum.REGISTER_EDI_SERVICE_REQ, "REGISTER_EDI_SERVICE_RS"),
	NET_ATTACH_REQ(WebSessionReqCmdNum.NET_ATTACH_REQ, "NET_ATTACH_RQ");

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

			case WebSessionReqCmdNum.LOGIN_REQ:
				result = WebSessionReqCmdEnum.LOGIN_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_GETTER_REQ:
				result = WebSessionReqCmdEnum.OBJECT_GETTER_REQ;
				break;

			//			case WebSessionReqCmdNum.OBJECT_GETBYID_REQ:
			//				result = WebSessionReqCmdEnum.OBJECT_GETBYID_REQ;
			//				break;

			case WebSessionReqCmdNum.OBJECT_UPDATE_REQ:
				result = WebSessionReqCmdEnum.OBJECT_UPDATE_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_DELETE_REQ:
				result = WebSessionReqCmdEnum.OBJECT_DELETE_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_LISTENER_REQ:
				result = WebSessionReqCmdEnum.OBJECT_LISTENER_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_FILTER_REQ:
				result = WebSessionReqCmdEnum.OBJECT_FILTER_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_METHOD_REQ:
				result = WebSessionReqCmdEnum.OBJECT_METHOD_REQ;
				break;

			case WebSessionReqCmdNum.REGISTER_EDI_SERVICE_REQ:
				result = WebSessionReqCmdEnum.REGISTER_EDI_SERVICE_REQ;
				break;

			case WebSessionReqCmdNum.NET_ATTACH_REQ:
				result = WebSessionReqCmdEnum.NET_ATTACH_REQ;
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

	public static WebSessionReqCmdEnum fromString(String inEnumNameStr) {
		if (inEnumNameStr != null) {
			for (WebSessionReqCmdEnum enumEntry : WebSessionReqCmdEnum.values()) {
				if (inEnumNameStr.equalsIgnoreCase(enumEntry.getName())) {
					return enumEntry;
				}
			}
		}
		return null;
	}

	final static class WebSessionReqCmdNum {
		static final byte	INVALID						= 0;
		static final byte	LOGIN_REQ					= 1;
		static final byte	OBJECT_GETTER_REQ			= 2;
		static final byte	OBJECT_GETBYID_REQ			= 3;
		static final byte	OBJECT_UPDATE_REQ			= 4;
		static final byte	OBJECT_DELETE_REQ			= 5;
		static final byte	OBJECT_LISTENER_REQ			= 6;
		static final byte	OBJECT_FILTER_REQ			= 7;
		static final byte	OBJECT_METHOD_REQ			= 8;
		static final byte	REGISTER_EDI_SERVICE_REQ	= 9;
		static final byte	NET_ATTACH_REQ				= 10;

		private WebSessionReqCmdNum() {

		}
	}
}
