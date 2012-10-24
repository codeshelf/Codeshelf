/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionPlanEnum.java,v 1.3 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum WorkInstructionPlanEnum {
	@EnumValue("INVALID")
	INVALID(WorkInstructionPlanNum.INVALID, "INVALID"),
	@EnumValue("PLANNED")
	PLANNED(WorkInstructionPlanNum.PLANNED, "PLANNED"),
	@EnumValue("ACTUAL")
	ACTUAL(WorkInstructionPlanNum.ACTUAL, "ACTUAL");

	private int		mValue;
	private String	mName;

	WorkInstructionPlanEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionPlanEnum getWorkInstructionPlanEnum(int inOnlineStatusID) {
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
