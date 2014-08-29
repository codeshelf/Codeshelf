/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionOpEnum.java,v 1.4 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum WorkInstructionOpEnum {
	// @EnumValue("INVALID")
	INVALID(WorkInstructionOpNum.INVALID, "INVALID"),
	// @EnumValue("CONTAINER_MOVE")
	CONTAINER_MOVE(WorkInstructionOpNum.CONTAINER_MOVE, "CONTAINER_MOVE"),
	// @EnumValue("ITEM_MOVE")
	ITEM_MOVE(WorkInstructionOpNum.ITEM_MOVE, "ITEM_MOVE");

	private int		mValue;
	private String	mName;

	WorkInstructionOpEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionOpEnum getWorkInstructionOpEnum(int inOnlineStatusID) {
		WorkInstructionOpEnum result;

		switch (inOnlineStatusID) {
			case WorkInstructionOpNum.CONTAINER_MOVE:
				result = WorkInstructionOpEnum.CONTAINER_MOVE;
				break;

			case WorkInstructionOpNum.ITEM_MOVE:
				result = WorkInstructionOpEnum.ITEM_MOVE;
				break;

			default:
				result = WorkInstructionOpEnum.INVALID;
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

	static final class WorkInstructionOpNum {

		static final byte	INVALID			= -1;
		static final byte	CONTAINER_MOVE	= 0;
		static final byte	ITEM_MOVE		= 1;

		private WorkInstructionOpNum() {
		};
	}
}
