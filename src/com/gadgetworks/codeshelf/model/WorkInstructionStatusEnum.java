/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionStatusEnum.java,v 1.1 2012/10/01 07:16:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NEW=NEW, INPROGRESS=INPROGRESS, COMPLETE=COMPLETE, REVERTED=REVERTED")
public enum WorkInstructionStatusEnum {
	INVALID(WorkInstructionStateNum.INVALID, "INVALID"),
	NEW(WorkInstructionStateNum.NEW, "NEW"),
	INPROGRESS(WorkInstructionStateNum.INPROGRESS, "INPROGRESS"),
	COMPLETE(WorkInstructionStateNum.COMPLETE, "COMPLETE"),
	REVERTED(WorkInstructionStateNum.REVERTED, "REVERTED");

	private int		mValue;
	private String	mName;

	WorkInstructionStatusEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionStatusEnum getWorkInstructionStateEnum(int inOnlineStatusID) {
		WorkInstructionStatusEnum result;

		switch (inOnlineStatusID) {
			case WorkInstructionStateNum.NEW:
				result = WorkInstructionStatusEnum.NEW;
				break;

			case WorkInstructionStateNum.INPROGRESS:
				result = WorkInstructionStatusEnum.INPROGRESS;
				break;

			case WorkInstructionStateNum.COMPLETE:
				result = WorkInstructionStatusEnum.COMPLETE;
				break;

			case WorkInstructionStateNum.REVERTED:
				result = WorkInstructionStatusEnum.REVERTED;
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

	static final class WorkInstructionStateNum {

		static final byte	INVALID		= 0;
		static final byte	NEW			= 1;
		static final byte	INPROGRESS	= 2;
		static final byte	COMPLETE	= 3;
		static final byte	REVERTED	= 4;

		private WorkInstructionStateNum() {
		};
	}
}
