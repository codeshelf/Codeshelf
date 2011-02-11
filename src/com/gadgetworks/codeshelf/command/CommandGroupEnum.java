/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandGroupEnum.java,v 1.4 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum CommandGroupEnum {

	INVALID(CommandGroupNum.INVALID, "INVALID"),
	NETMGMT(CommandGroupNum.NETMGMT, "NETMGMT"),
	ASSOC(CommandGroupNum.ASSOC, "ASSOC"),
	INFO(CommandGroupNum.INFO, "INFO"),
	CONTROL(CommandGroupNum.CONTROL, "CONTROL"),
	ATOP(CommandGroupNum.ATOP, "ATOP"),
	CS(CommandGroupNum.CS, "CS");

	private byte	mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	CommandGroupEnum(final byte inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommandID
	 *  @return
	 */
	public static CommandGroupEnum getCommandGroupEnum(int inCommandID) {
		CommandGroupEnum result = CommandGroupEnum.INVALID;

		switch (inCommandID) {
			case CommandGroupNum.NETMGMT:
				result = CommandGroupEnum.NETMGMT;
				break;
			case CommandGroupNum.ASSOC:
				result = CommandGroupEnum.ASSOC;
				break;
			case CommandGroupNum.INFO:
				result = CommandGroupEnum.INFO;
				break;
			case CommandGroupNum.CONTROL:
				result = CommandGroupEnum.CONTROL;
				break;
			case CommandGroupNum.ATOP:
				result = CommandGroupEnum.ATOP;
				break;
			case CommandGroupNum.CS:
				result = CommandGroupEnum.CS;
				break;
			default:
				break;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getValue() {
		return mValue;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getName() {
		return mName;
	}

	static final class CommandGroupNum {
		static final byte	INVALID	= 0;
		static final byte	NETMGMT	= 0;
		static final byte	ASSOC	= 1;
		static final byte	INFO	= 2;
		static final byte	CONTROL	= 3;
		static final byte	ATOP	= 4;
		static final byte	CS		= 5;

		public CommandGroupNum() {

		};
	}

}
