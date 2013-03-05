/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionRespCmdEnum.java,v 1.11 2013/03/05 07:47:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;


/**
 * @author jeffw
 *
 */
public enum WebSessionRespCmdEnum {
	INVALID(WebSessionRespCmdNum.INVALID, "INVALID"),
	LOGIN_RESP(WebSessionRespCmdNum.LOGIN_RESP, "LOGIN_RS"),
	OBJECT_GETTER_RESP(WebSessionRespCmdNum.OBJECT_GETTER_RESP, "OBJ_GET_RS"),
	OBJECT_UPDATE_RESP(WebSessionRespCmdNum.OBJECT_UPDATE_RESP, "OBJ_UPD_RS"),
	OBJECT_DELETE_RESP(WebSessionRespCmdNum.OBJECT_DELETE_RESP, "OBJ_DEL_RS"),
	OBJECT_LISTENER_RESP(WebSessionRespCmdNum.OBJECT_LISTENER_RESP, "OBJ_LSN_RS"),
	OBJECT_FILTER_RESP(WebSessionRespCmdNum.OBJECT_FILTER_RESP, "OBJ_FLT_RS"),
	OBJECT_METHOD_RESP(WebSessionRespCmdNum.OBJECT_METHOD_RESP, "OBJ_METH_RS"),
	REGISTER_EDI_RESP(WebSessionRespCmdNum.REGISTER_EDI_RESP, "REGISTER_EDI_RS"),
	NET_ATTACH_RESP(WebSessionRespCmdNum.NET_ATTACH_RESP, "ATTACH_RS"),
	CHE_WORK_RESP(WebSessionRespCmdNum.CHE_WORK_RESP, "CHE_WORK_RS");

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

			case WebSessionRespCmdNum.LOGIN_RESP:
				result = WebSessionRespCmdEnum.LOGIN_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_GETTER_RESP:
				result = WebSessionRespCmdEnum.OBJECT_GETTER_RESP;
				break;

			//			case WebSessionRespCmdNum.OBJECT_GETBYID_RESP:
			//				result = WebSessionRespCmdEnum.OBJECT_GETBYID_RESP;
			//				break;

			case WebSessionRespCmdNum.OBJECT_UPDATE_RESP:
				result = WebSessionRespCmdEnum.OBJECT_UPDATE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_DELETE_RESP:
				result = WebSessionRespCmdEnum.OBJECT_DELETE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_LISTENER_RESP:
				result = WebSessionRespCmdEnum.OBJECT_LISTENER_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_METHOD_RESP:
				result = WebSessionRespCmdEnum.OBJECT_METHOD_RESP;
				break;

			case WebSessionRespCmdNum.REGISTER_EDI_RESP:
				result = WebSessionRespCmdEnum.REGISTER_EDI_RESP;
				break;

			case WebSessionRespCmdNum.NET_ATTACH_RESP:
				result = WebSessionRespCmdEnum.NET_ATTACH_RESP;
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

	public String toString() {
		return getName();
	}

	public static WebSessionRespCmdEnum fromString(String inEnumNameStr) {
		if (inEnumNameStr != null) {
			for (WebSessionRespCmdEnum enumEntry : WebSessionRespCmdEnum.values()) {
				if (inEnumNameStr.equalsIgnoreCase(enumEntry.getName())) {
					return enumEntry;
				}
			}
		}
		return null;
	}

	final static class WebSessionRespCmdNum {
		static final byte	INVALID					= 0;
		static final byte	LOGIN_RESP				= 1;
		static final byte	OBJECT_GETTER_RESP		= 2;
		static final byte	OBJECT_GETBYID_RESP		= 3;
		static final byte	OBJECT_UPDATE_RESP		= 4;
		static final byte	OBJECT_DELETE_RESP		= 5;
		static final byte	OBJECT_LISTENER_RESP	= 6;
		static final byte	OBJECT_FILTER_RESP		= 7;
		static final byte	OBJECT_METHOD_RESP		= 8;
		static final byte	REGISTER_EDI_RESP		= 9;
		static final byte	NET_ATTACH_RESP			= 10;
		static final byte	CHE_WORK_RESP			= 11;

		private WebSessionRespCmdNum() {

		}
	}
}
