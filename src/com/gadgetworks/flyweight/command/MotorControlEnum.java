/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: MotorControlEnum.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.command;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum MotorControlEnum {

	INVALID(MotorCommandNum.INVALID, "INVALID"),
	RUN_FORWARD(MotorCommandNum.RUN_FORWARD, "RUN_FORWARD"),
	RUN_BACKWARD(MotorCommandNum.RUN_BACKWARD, "RUN_BACKWARD"),
	FREEWHEEL(MotorCommandNum.FREEWHEEL, "FREEWHEEL"),
	BRAKE(MotorCommandNum.BRAKE, "BRAKE");

	private int		mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	MotorControlEnum(final int inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inMotorControlCommandID
	 *  @return
	 */
	public static MotorControlEnum getCommandEnum(int inMotorControlCommandID) {
		MotorControlEnum result = MotorControlEnum.INVALID;

		switch (inMotorControlCommandID) {
			case MotorCommandNum.RUN_FORWARD:
				result = MotorControlEnum.RUN_FORWARD;
				break;
			case MotorCommandNum.RUN_BACKWARD:
				result = MotorControlEnum.RUN_BACKWARD;
				break;
			case MotorCommandNum.FREEWHEEL:
				result = MotorControlEnum.FREEWHEEL;
				break;
			case MotorCommandNum.BRAKE:
				result = MotorControlEnum.BRAKE;
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
	
	final static class MotorCommandNum {
		static final int	INVALID			= 0;
		static final int	FREEWHEEL		= 0;
		static final int	RUN_FORWARD		= 1;
		static final int	RUN_BACKWARD	= 2;
		static final int	BRAKE			= 3;
		
		private MotorCommandNum() {
			
		};
	}
}
