/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionTypeEnum.java,v 1.3 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum WorkInstructionTypeEnum {
	@EnumValue("INVALID")
	INVALID(WorkInstructionTypeNum.INVALID, "INVALID"),
	@EnumValue("PLAN")
	PLAN(WorkInstructionTypeNum.PLAN, "PLAN"),
	@EnumValue("ACTUAL")
	ACTUAL(WorkInstructionTypeNum.ACTUAL, "ACTUAL"),
	@EnumValue("INDICATOR")
	INDICATOR(WorkInstructionTypeNum.INDICATOR, "INDICATOR");

	private int		mValue;
	private String	mName;

	WorkInstructionTypeEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static WorkInstructionTypeEnum getWorkInstructionTypeEnum(int inOnlineStatusID) {
		WorkInstructionTypeEnum result;

		switch (inOnlineStatusID) {
			case WorkInstructionTypeNum.PLAN:
				result = WorkInstructionTypeEnum.PLAN;
				break;

			case WorkInstructionTypeNum.ACTUAL:
				result = WorkInstructionTypeEnum.ACTUAL;
				break;

			case WorkInstructionTypeNum.INDICATOR:
				result = WorkInstructionTypeEnum.INDICATOR;
				break;

			default:
				result = WorkInstructionTypeEnum.INVALID;
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

	static final class WorkInstructionTypeNum {

		static final byte	INVALID		= -1;
		static final byte	PLAN		= 0;
		static final byte	ACTUAL		= 1;
		static final byte	INDICATOR	= 2;

		private WorkInstructionTypeNum() {
		};
	}
}
