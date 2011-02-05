/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandIdEnum.java,v 1.4 2011/02/05 01:41:56 jeffw Exp $
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
	ATOP_ALPHA_NUM_PUSH(CommandIdNum.ATOP_ALPHA_NUM_PUSH, "ATOP_ALPHA_NUM_PUSH"),
	ATOP_ALPHA_NUM_CLEAR(CommandIdNum.ATOP_ALPHA_NUM_CLEAR, "ATOP_ALPHA_NUM_CLEAR"),
	ATOP_LED_ON(CommandIdNum.ATOP_LED_ON, "ATOP_LED_ON"),
	ATOP_LED_OFF(CommandIdNum.ATOP_LED_OFF, "ATOP_LED_OFF"),
	ATOP_SET_MAX_DEVICES(CommandIdNum.ATOP_SET_MAX_DEVICES, "ATOP_SET_MAX_DEVICES"),
	ATOP_READ_ALL_STATUS(CommandIdNum.ATOP_READ_ALL_STATUS, "ATOP_READ_ALL_STATUS"),
	ATOP_PICK_MODE(CommandIdNum.ATOP_PICK_MODE, "ATOP_PICK_MODE"),
	ATOP_DIGIT_LIMIT(CommandIdNum.ATOP_DIGIT_LIMIT, "ATOP_DIGIT_LIMIT"),
	ATOP_LED_COLOR(CommandIdNum.ATOP_LED_COLOR, "ATOP_LED_COLOR");

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
		static final int	INVALID			= 0;
		static final int	ACK				= 1;
		static final int	ASSOC_REQ		= 2;
		static final int	ASSOC_RESP		= 3;
		static final int	ASSOC_CHECK		= 4;
		static final int	ASSOC_ACK		= 5;
		static final int	QUERY			= 6;
		static final int	RESPONSE		= 7;
		static final int	NET_SETUP		= 8;
		static final int	NET_CHECK		= 9;
		static final int	BUTTON			= 10;
		static final byte	ATOP_ALPHA_NUM_PUSH	= 11;
		static final byte	ATOP_ALPHA_NUM_CLEAR	= 12;
		static final byte	ATOP_LED_ON			= 13;
		static final byte	ATOP_LED_OFF			= 14;
		static final byte	ATOP_SET_MAX_DEVICES	= 15;
		static final byte	ATOP_READ_ALL_STATUS	= 16;
		static final byte	ATOP_PICK_MODE		= 17;
		static final byte	ATOP_DIGIT_LIMIT		= 18;
		static final byte	ATOP_LED_COLOR		= 19;

		private CommandIdNum() {

		};
	}

}
