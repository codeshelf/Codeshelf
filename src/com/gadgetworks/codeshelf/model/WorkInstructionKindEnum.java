/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionKindEnum.java,v 1.1 2012/10/01 01:35:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, PLANNED=PLANNED, ACTUAL=ACTUAL")
public enum WorkInstructionKindEnum {
	INVALID(WorkInstructionKindNum.INVALID, "INVALID"),
	PLANNED(WorkInstructionKindNum.PLANNED, "PLANNED"),
	ACTUAL(WorkInstructionKindNum.ACTUAL, "ACTUAL");

	private int		mValue;
	private String	mName;

	WorkInstructionKindEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionKindEnum getWorkInstructionKindEnum(int inOnlineStatusID) {
		WorkInstructionKindEnum result;

		switch (inOnlineStatusID) {
			case WorkInstructionKindNum.PLANNED:
				result = WorkInstructionKindEnum.PLANNED;
				break;

			case WorkInstructionKindNum.ACTUAL:
				result = WorkInstructionKindEnum.ACTUAL;
				break;

			default:
				result = WorkInstructionKindEnum.INVALID;
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

	static final class WorkInstructionKindNum {

		static final byte	INVALID	= 0;
		static final byte	PLANNED	= 1;
		static final byte	ACTUAL	= 2;

		private WorkInstructionKindNum() {
		};
	}
}
