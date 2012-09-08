/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandIdEnum.java,v 1.11 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

public enum CommandIdEnum {
	INVALID(CommandIdNum.INVALID, "INVALID"),
	ACK(CommandIdNum.ACK, "ACK"),
	ASSOC_REQ(CommandIdNum.ASSOC_REQ, "ASSOC_REQ"),
	ASSOC_RESP(CommandIdNum.ASSOC_RESP, "ASSOC_RESP"),
	ASSOC_CHECK(CommandIdNum.ASSOC_CHECK, "ASSOC_CHECK"),
	ASSOC_ACK(CommandIdNum.ASSOC_ACK, "ASSOC_ACK"),
	QUERY(CommandIdNum.QUERY, "QUERY"),
	RESPONSE(CommandIdNum.RESPONSE, "RESPONSE"),
	NET_SETUP(CommandIdNum.NET_SETUP, "NET_SETUP"),
	NET_CHECK(CommandIdNum.NET_CHECK, "NET_CHECK"),
	BUTTON(CommandIdNum.BUTTON, "BUTTON"),
	CS_DISPLAY_CLEAR(CommandIdNum.CS_DISPLAY_CLEAR, "displayClear"),
	CS_DISPLAY_TEXT(CommandIdNum.CS_DISPLAY_TEXT, "displayText"),
	CS_SET_COUNT(CommandIdNum.CS_SET_COUNT, "setCount"),
	CS_INDICATOR_ON(CommandIdNum.CS_INDICATOR_ON, "indicatorOn"),
	CS_INDICATOR_OFF(CommandIdNum.CS_INDICATOR_OFF, "indicatorOn"),
	CS_INDICATOR_BLINK(CommandIdNum.CS_INDICATOR_BLINK, "indicatorBlink"),
	CS_REPORT_PICK(CommandIdNum.CS_REPORT_PICK, "reportPick"),
	CS_REPORT_SHORT(CommandIdNum.CS_REPORT_SHORT, "reportShort"),
	CS_ACK_PRESSED(CommandIdNum.CS_ACK_PRESSED, "ackPressed");

	private int		mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	CommandIdEnum(final int inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommandID
	 *  @return
	 */
	//	public static CommandIdEnum getCommandIdEnum(int inCommandId) {
	//		CommandIdEnum result = CommandIdEnum.INVALID;
	//
	//		switch (inCommandId) {
	//			case CommandIdNum.ACK:
	//				result = CommandIdEnum.ACK;
	//				break;
	//			case CommandIdNum.ASSOC_REQ:
	//				result = CommandIdEnum.ASSOC_REQ;
	//				break;
	//			case CommandIdNum.ASSOC_RESP:
	//				result = CommandIdEnum.ASSOC_RESP;
	//				break;
	//			case CommandIdNum.ASSOC_CHECK:
	//				result = CommandIdEnum.ASSOC_CHECK;
	//				break;
	//			case CommandIdNum.ASSOC_ACK:
	//				result = CommandIdEnum.ASSOC_ACK;
	//				break;
	//			case CommandIdNum.QUERY:
	//				result = CommandIdEnum.QUERY;
	//				break;
	//			case CommandIdNum.RESPONSE:
	//				result = CommandIdEnum.RESPONSE;
	//				break;
	//			case CommandIdNum.NET_SETUP:
	//				result = CommandIdEnum.NET_SETUP;
	//				break;
	//			case CommandIdNum.NET_CHECK:
	//				result = CommandIdEnum.NET_CHECK;
	//				break;
	//			case CommandIdNum.BUTTON:
	//				result = CommandIdEnum.BUTTON;
	//				break;
	//			default:
	//				break;
	//		}
	//
	//		return result;
	//	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public int getValue() {
		return mValue;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getName() {
		return mName;
	}

	static final class CommandIdNum {
		static final int	INVALID				= 0;
		static final int	ACK					= 1;
		static final int	ASSOC_REQ			= 2;
		static final int	ASSOC_RESP			= 3;
		static final int	ASSOC_CHECK			= 4;
		static final int	ASSOC_ACK			= 5;
		static final int	QUERY				= 6;
		static final int	RESPONSE			= 7;
		static final int	NET_SETUP			= 8;
		static final int	NET_CHECK			= 9;
		static final int	BUTTON				= 10;
		static final byte	CS_DISPLAY_CLEAR	= 11;
		static final byte	CS_DISPLAY_TEXT		= 12;
		static final byte	CS_SET_COUNT		= 13;
		static final byte	CS_INDICATOR_ON		= 14;
		static final byte	CS_INDICATOR_OFF	= 15;
		static final byte	CS_INDICATOR_BLINK	= 16;
		static final byte	CS_ACK_PRESSED		= 17;
		static final byte	CS_REPORT_PICK		= 18;
		static final byte	CS_REPORT_SHORT		= 19;

		private CommandIdNum() {

		};
	}
}
