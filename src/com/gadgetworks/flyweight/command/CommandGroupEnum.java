/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandGroupEnum.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.command;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum CommandGroupEnum {

	INVALID(CommandGroupNum.INVALID, "INVALID"),
	NETMGMT(CommandGroupNum.NETMGMT, "NETMGMT"),
	ASSOC(CommandGroupNum.ASSOC, "ASSOC"),
	CONTROL(CommandGroupNum.CONTROL, "CONTROL");

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
			case CommandGroupNum.CONTROL:
				result = CommandGroupEnum.CONTROL;
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

	final static class CommandGroupNum {
		static final byte	INVALID	= 0;
		static final byte	NETMGMT	= 1;
		static final byte	ASSOC	= 2;
		static final byte	CONTROL	= 3;

		public CommandGroupNum() {

		};
	}

}
