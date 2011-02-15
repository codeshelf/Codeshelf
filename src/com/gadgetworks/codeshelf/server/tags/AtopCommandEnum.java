/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCommandEnum.java,v 1.1 2011/02/15 02:39:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum AtopCommandEnum {

	INVALID(AtopCommandNum.INVALID, "INVALID"),
	ALPHA_NUM_PUSH(AtopCommandNum.ALPHA_NUM_PUSH, "ALPHA_NUM_PUSH"),
	ALPHA_NUM_CLEAR(AtopCommandNum.ALPHA_NUM_CLEAR, "ALPHA_NUM_CLEAR"),
	LED_ON(AtopCommandNum.LED_ON, "LED_ON"),
	LED_OFF(AtopCommandNum.LED_OFF, "LED_OFF"),
	SET_MAX_DEVICES(AtopCommandNum.SET_MAX_DEVICES, "SET_MAX_DEVICES"),
	READ_ALL_STATUS(AtopCommandNum.READ_ALL_STATUS, "READ_ALL_STATUS"),
	PICK_MODE(AtopCommandNum.PICK_MODE, "PICK_MODE"),
	DIGIT_LIMIT(AtopCommandNum.DIGIT_LIMIT, "DIGIT_LIMIT"),
	TAG_CONFIG(AtopCommandNum.TAG_CONFIG, "TAG_CONFIG"),
	SPECIAL_RETURN(AtopCommandNum.SPECIAL_RETURN, "SPECIAL_RETURN");

	private byte	mValue;
	private String	mName;

	AtopCommandEnum(final byte inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static AtopCommandEnum getCommandEnum(int inCommandNum) {
		AtopCommandEnum result;

		switch (inCommandNum) {
			case AtopCommandNum.ALPHA_NUM_PUSH:
				result = AtopCommandEnum.ALPHA_NUM_PUSH;
				break;

			case AtopCommandNum.ALPHA_NUM_CLEAR:
				result = AtopCommandEnum.ALPHA_NUM_CLEAR;
				break;

			case AtopCommandNum.LED_ON:
				result = AtopCommandEnum.LED_ON;
				break;

			case AtopCommandNum.LED_OFF:
				result = AtopCommandEnum.LED_OFF;
				break;

			case AtopCommandNum.SET_MAX_DEVICES:
				result = AtopCommandEnum.SET_MAX_DEVICES;
				break;

			case AtopCommandNum.READ_ALL_STATUS:
				result = AtopCommandEnum.READ_ALL_STATUS;
				break;

			case AtopCommandNum.PICK_MODE:
				result = AtopCommandEnum.PICK_MODE;
				break;

			case AtopCommandNum.DIGIT_LIMIT:
				result = AtopCommandEnum.DIGIT_LIMIT;
				break;

			case AtopCommandNum.TAG_CONFIG:
				result = AtopCommandEnum.TAG_CONFIG;
				break;

			case AtopCommandNum.SPECIAL_RETURN:
				result = AtopCommandEnum.SPECIAL_RETURN;
				break;

			default:
				result = AtopCommandEnum.INVALID;
				break;
		}

		return result;
	}

	public byte getValue() {
		return mValue;
	}

	public String getName() {
		return mName;
	}

	final static class AtopCommandNum {

		static final byte	INVALID			= 0;
		static final byte	ALPHA_NUM_PUSH	= 0x00;
		static final byte	ALPHA_NUM_CLEAR	= 0x01;
		static final byte	LED_ON			= 0x02;
		static final byte	LED_OFF			= 0x03;
		static final byte	SET_MAX_DEVICES	= 0x08;
		static final byte	READ_ALL_STATUS	= 0x09;
		static final byte	PICK_MODE		= 0x1a;
		static final byte	DIGIT_LIMIT		= 0x1e;
		static final byte	TAG_CONFIG		= 0x1f;
		static final byte	SPECIAL_RETURN	= 0x64;

		private AtopCommandNum() {

		}
	}

}
