/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionStateEnum.java,v 1.1 2012/10/01 01:35:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NEW=NEW, INPROGRESS=INPROGRESS, COMPLETE=COMPLETE, REVERTED=REVERTED")
public enum WorkInstructionStateEnum {
	INVALID(WorkInstructionStateNum.INVALID, "INVALID"),
	NEW(WorkInstructionStateNum.NEW, "NEW"),
	INPROGRESS(WorkInstructionStateNum.INPROGRESS, "INPROGRESS"),
	COMPLETE(WorkInstructionStateNum.COMPLETE, "COMPLETE"),
	REVERTED(WorkInstructionStateNum.REVERTED, "REVERTED");

	private int		mValue;
	private String	mName;

	WorkInstructionStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionStateEnum getWorkInstructionStateEnum(int inOnlineStatusID) {
		WorkInstructionStateEnum result;

		switch (inOnlineStatusID) {
			case WorkInstructionStateNum.NEW:
				result = WorkInstructionStateEnum.NEW;
				break;

			case WorkInstructionStateNum.INPROGRESS:
				result = WorkInstructionStateEnum.INPROGRESS;
				break;

			case WorkInstructionStateNum.COMPLETE:
				result = WorkInstructionStateEnum.COMPLETE;
				break;

			case WorkInstructionStateNum.REVERTED:
				result = WorkInstructionStateEnum.REVERTED;
				break;

			default:
				result = WorkInstructionStateEnum.INVALID;
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
