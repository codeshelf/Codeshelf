/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionOpEnum.java,v 1.1 2012/10/01 07:16:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, CONTAINER_MOVE=CONTAINER_MOVE, ITEM_MOVE=ITEM_MOVE")
public enum WorkInstructionOpEnum {
	INVALID(WorkInstructionOpNum.INVALID, "INVALID"),
	CONTAINER_MOVE(WorkInstructionOpNum.CONTAINER_MOVE, "CONTAINER_MOVE"),
	ITEM_MOVE(WorkInstructionOpNum.ITEM_MOVE, "ITEM_MOVE");

	private int		mValue;
	private String	mName;

	WorkInstructionOpEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionOpEnum getWorkInstructionStateEnum(int inOnlineStatusID) {
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

		static final byte	INVALID			= 0;
		static final byte	CONTAINER_MOVE	= 1;
		static final byte	ITEM_MOVE		= 2;

		private WorkInstructionOpNum() {
		};
	}
}
