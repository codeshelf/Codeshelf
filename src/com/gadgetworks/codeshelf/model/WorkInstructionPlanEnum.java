/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionPlanEnum.java,v 1.1 2012/10/01 07:16:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, PLANNED=PLANNED, ACTUAL=ACTUAL")
public enum WorkInstructionPlanEnum {
	INVALID(WorkInstructionPlanNum.INVALID, "INVALID"),
	PLANNED(WorkInstructionPlanNum.PLANNED, "PLANNED"),
	ACTUAL(WorkInstructionPlanNum.ACTUAL, "ACTUAL");

	private int		mValue;
	private String	mName;

	WorkInstructionPlanEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionPlanEnum getWorkInstructionKindEnum(int inOnlineStatusID) {
		WorkInstructionPlanEnum result;

		switch (inOnlineStatusID) {
			case WorkInstructionPlanNum.PLANNED:
				result = WorkInstructionPlanEnum.PLANNED;
				break;

			case WorkInstructionPlanNum.ACTUAL:
				result = WorkInstructionPlanEnum.ACTUAL;
				break;

			default:
				result = WorkInstructionPlanEnum.INVALID;
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

	static final class WorkInstructionPlanNum {

		static final byte	INVALID	= 0;
		static final byte	PLANNED	= 1;
		static final byte	ACTUAL	= 2;

		private WorkInstructionPlanNum() {
		};
	}
}
