/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionStatusEnum.java,v 1.6 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum WorkInstructionStatusEnum {
	// @EnumValue("INVALID")
	INVALID(WorkInstructionStatusNum.INVALID, "INVALID"),
	// @EnumValue("NEW")
	NEW(WorkInstructionStatusNum.NEW, "NEW"),
	// @EnumValue("INPROGRESS")
	INPROGRESS(WorkInstructionStatusNum.INPROGRESS, "INPROGRESS"),
	// @EnumValue("SHORT")
	SHORT(WorkInstructionStatusNum.SHORT, "SHORT"),
	// @EnumValue("COMPLETE")
	COMPLETE(WorkInstructionStatusNum.COMPLETE, "COMPLETE"),
	// @EnumValue("REVERT")
	REVERT(WorkInstructionStatusNum.REVERT, "REVERT");

	private int		mValue;
	private String	mName;

	WorkInstructionStatusEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionStatusEnum getWorkInstructionStatusEnum(int inOnlineStatusID) {
		WorkInstructionStatusEnum result;

		switch (inOnlineStatusID) {
			case WorkInstructionStatusNum.NEW:
				result = WorkInstructionStatusEnum.NEW;
				break;

			case WorkInstructionStatusNum.INPROGRESS:
				result = WorkInstructionStatusEnum.INPROGRESS;
				break;

			case WorkInstructionStatusNum.SHORT:
				result = WorkInstructionStatusEnum.SHORT;
				break;

			case WorkInstructionStatusNum.COMPLETE:
				result = WorkInstructionStatusEnum.COMPLETE;
				break;

			case WorkInstructionStatusNum.REVERT:
				result = WorkInstructionStatusEnum.REVERT;
				break;

			default:
				result = WorkInstructionStatusEnum.INVALID;
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

	static final class WorkInstructionStatusNum {

		static final byte	INVALID		= -1;
		static final byte	NEW			= 0;
		static final byte	INPROGRESS	= 1;
		static final byte	SHORT		= 2;
		static final byte	COMPLETE	= 3;
		static final byte	REVERT		= 4;

		private WorkInstructionStatusNum() {
		};
	}
}
