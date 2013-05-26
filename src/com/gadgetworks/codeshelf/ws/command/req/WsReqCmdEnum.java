/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WsReqCmdEnum.java,v 1.2 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

/**
 * @author jeffw
 *
 */
public enum WsReqCmdEnum {
	INVALID(WebSessionReqCmdNum.INVALID, "INVALID"),
	LOGIN_REQ(WebSessionReqCmdNum.LOGIN_REQ, "LOGIN_RQ"),
	OBJECT_GETTER_REQ(WebSessionReqCmdNum.OBJECT_GETTER_REQ, "OBJ_GET_RQ"),
	OBJECT_UPDATE_REQ(WebSessionReqCmdNum.OBJECT_UPDATE_REQ, "OBJ_UPD_RQ"),
	OBJECT_DELETE_REQ(WebSessionReqCmdNum.OBJECT_DELETE_REQ, "OBJ_DEL_RQ"),
	OBJECT_LISTENER_REQ(WebSessionReqCmdNum.OBJECT_LISTENER_REQ, "OBJ_LSN_RQ"),
	OBJECT_FILTER_REQ(WebSessionReqCmdNum.OBJECT_FILTER_REQ, "OBJ_FLT_RQ"),
	OBJECT_METHOD_REQ(WebSessionReqCmdNum.OBJECT_METHOD_REQ, "OBJ_METH_RQ"),
	REGISTER_EDI_SERVICE_REQ(WebSessionReqCmdNum.REGISTER_EDI_SERVICE_REQ, "REGISTER_EDI_SERVICE_RS"),
	NET_ATTACH_REQ(WebSessionReqCmdNum.NET_ATTACH_REQ, "NET_ATTACH_RQ"),
	CHE_WORK_REQ(WebSessionReqCmdNum.CHE_WORK_REQ, "CHE_WORK_RQ"),
	CHE_WICOMPLETE_REQ(WebSessionReqCmdNum.CHE_WICOMPLETE_REQ, "CHE_WICOMP_RQ");

	private int		mValue;
	private String	mName;

	WsReqCmdEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WsReqCmdEnum getWebSessionReqCmdEnum(int inSessionCmdNum) {
		WsReqCmdEnum result;

		switch (inSessionCmdNum) {
			case WebSessionReqCmdNum.INVALID:
				result = WsReqCmdEnum.INVALID;
				break;

			case WebSessionReqCmdNum.LOGIN_REQ:
				result = WsReqCmdEnum.LOGIN_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_GETTER_REQ:
				result = WsReqCmdEnum.OBJECT_GETTER_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_UPDATE_REQ:
				result = WsReqCmdEnum.OBJECT_UPDATE_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_DELETE_REQ:
				result = WsReqCmdEnum.OBJECT_DELETE_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_LISTENER_REQ:
				result = WsReqCmdEnum.OBJECT_LISTENER_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_FILTER_REQ:
				result = WsReqCmdEnum.OBJECT_FILTER_REQ;
				break;

			case WebSessionReqCmdNum.OBJECT_METHOD_REQ:
				result = WsReqCmdEnum.OBJECT_METHOD_REQ;
				break;

			case WebSessionReqCmdNum.REGISTER_EDI_SERVICE_REQ:
				result = WsReqCmdEnum.REGISTER_EDI_SERVICE_REQ;
				break;

			case WebSessionReqCmdNum.NET_ATTACH_REQ:
				result = WsReqCmdEnum.NET_ATTACH_REQ;
				break;

			case WebSessionReqCmdNum.CHE_WORK_REQ:
				result = WsReqCmdEnum.CHE_WORK_REQ;
				break;

			case WebSessionReqCmdNum.CHE_WICOMPLETE_REQ:
				result = WsReqCmdEnum.CHE_WICOMPLETE_REQ;
				break;

			default:
				result = WsReqCmdEnum.INVALID;
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

	public static WsReqCmdEnum fromString(String inEnumNameStr) {
		if (inEnumNameStr != null) {
			for (WsReqCmdEnum enumEntry : WsReqCmdEnum.values()) {
				if (inEnumNameStr.equalsIgnoreCase(enumEntry.getName())) {
					return enumEntry;
				}
			}
		}
		return null;
	}

	final static class WebSessionReqCmdNum {
		static final byte	INVALID						= -1;
		static final byte	LOGIN_REQ					= 2;
		static final byte	OBJECT_GETTER_REQ			= 1;
		static final byte	OBJECT_GETBYID_REQ			= 2;
		static final byte	OBJECT_UPDATE_REQ			= 3;
		static final byte	OBJECT_DELETE_REQ			= 4;
		static final byte	OBJECT_LISTENER_REQ			= 5;
		static final byte	OBJECT_FILTER_REQ			= 6;
		static final byte	OBJECT_METHOD_REQ			= 7;
		static final byte	REGISTER_EDI_SERVICE_REQ	= 8;
		static final byte	NET_ATTACH_REQ				= 9;
		static final byte	CHE_WORK_REQ				= 10;
		static final byte	CHE_WICOMPLETE_REQ			= 11;

		private WebSessionReqCmdNum() {

		}
	}
}
