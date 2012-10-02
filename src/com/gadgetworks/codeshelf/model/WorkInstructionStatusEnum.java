/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionStatusEnum.java,v 1.2 2012/10/02 15:12:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NEW=NEW, INPROGRESS=INPROGRESS, COMPLETE=COMPLETE, REVERTED=REVERTED")
public enum WorkInstructionStatusEnum {
	INVALID(WorkInstructionStatusNum.INVALID, "INVALID"),
	NEW(WorkInstructionStatusNum.NEW, "NEW"),
	INPROGRESS(WorkInstructionStatusNum.INPROGRESS, "INPROGRESS"),
	COMPLETE(WorkInstructionStatusNum.COMPLETE, "COMPLETE"),
	REVERTED(WorkInstructionStatusNum.REVERTED, "REVERTED");

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

			case WorkInstructionStatusNum.REVERTED:
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

	static final class WorkInstructionStatusNum {

		static final byte	INVALID		= 0;
		static final byte	NEW			= 1;
		static final byte	INPROGRESS	= 2;
		static final byte	COMPLETE	= 3;
		static final byte	REVERTED	= 4;

		private WorkInstructionStatusNum() {
		};
	}
}
