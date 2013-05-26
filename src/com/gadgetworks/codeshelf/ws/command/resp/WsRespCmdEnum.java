/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WsRespCmdEnum.java,v 1.2 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

/**
 * @author jeffw
 *
 */
public enum WsRespCmdEnum {
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

	WsRespCmdEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WsRespCmdEnum getWebSessionRespCmdEnum(int inSessionCmdNum) {
		WsRespCmdEnum result;

		switch (inSessionCmdNum) {
			case WebSessionRespCmdNum.INVALID:
				result = WsRespCmdEnum.INVALID;
				break;

			case WebSessionRespCmdNum.LOGIN_RESP:
				result = WsRespCmdEnum.LOGIN_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_GETTER_RESP:
				result = WsRespCmdEnum.OBJECT_GETTER_RESP;
				break;

			//			case WebSessionRespCmdNum.OBJECT_GETBYID_RESP:
			//				result = WebSessionRespCmdEnum.OBJECT_GETBYID_RESP;
			//				break;

			case WebSessionRespCmdNum.OBJECT_UPDATE_RESP:
				result = WsRespCmdEnum.OBJECT_UPDATE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_DELETE_RESP:
				result = WsRespCmdEnum.OBJECT_DELETE_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_LISTENER_RESP:
				result = WsRespCmdEnum.OBJECT_LISTENER_RESP;
				break;

			case WebSessionRespCmdNum.OBJECT_METHOD_RESP:
				result = WsRespCmdEnum.OBJECT_METHOD_RESP;
				break;

			case WebSessionRespCmdNum.REGISTER_EDI_RESP:
				result = WsRespCmdEnum.REGISTER_EDI_RESP;
				break;

			case WebSessionRespCmdNum.NET_ATTACH_RESP:
				result = WsRespCmdEnum.NET_ATTACH_RESP;
				break;

			default:
				result = WsRespCmdEnum.INVALID;
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

	public static WsRespCmdEnum fromString(String inEnumNameStr) {
		if (inEnumNameStr != null) {
			for (WsRespCmdEnum enumEntry : WsRespCmdEnum.values()) {
				if (inEnumNameStr.equalsIgnoreCase(enumEntry.getName())) {
					return enumEntry;
				}
			}
		}
		return null;
	}

	final static class WebSessionRespCmdNum {
		static final byte	INVALID					= -1;
		static final byte	LOGIN_RESP				= 0;
		static final byte	OBJECT_GETTER_RESP		= 1;
		static final byte	OBJECT_GETBYID_RESP		= 2;
		static final byte	OBJECT_UPDATE_RESP		= 3;
		static final byte	OBJECT_DELETE_RESP		= 4;
		static final byte	OBJECT_LISTENER_RESP	= 5;
		static final byte	OBJECT_FILTER_RESP		= 6;
		static final byte	OBJECT_METHOD_RESP		= 7;
		static final byte	REGISTER_EDI_RESP		= 8;
		static final byte	NET_ATTACH_RESP			= 9;
		static final byte	CHE_WORK_RESP			= 10;

		private WebSessionRespCmdNum() {

		}
	}
}
