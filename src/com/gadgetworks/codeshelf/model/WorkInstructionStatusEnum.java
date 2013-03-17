/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionStatusEnum.java,v 1.4 2013/03/17 23:10:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum WorkInstructionStatusEnum {
	@EnumValue("INVALID")
	INVALID(WorkInstructionStatusNum.INVALID, "INVALID"),
	@EnumValue("NEW")
	NEW(WorkInstructionStatusNum.NEW, "NEW"),
	@EnumValue("INPROGRESS")
	INPROGRESS(WorkInstructionStatusNum.INPROGRESS, "INPROGRESS"),
	@EnumValue("COMPLETE")
	COMPLETE(WorkInstructionStatusNum.COMPLETE, "COMPLETE"),
	@EnumValue("REVERT")
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

		static final byte	INVALID		= 0;
		static final byte	NEW			= 1;
		static final byte	INPROGRESS	= 2;
		static final byte	COMPLETE	= 3;
		static final byte	REVERT		= 4;

		private WorkInstructionStatusNum() {
		};
	}
}
