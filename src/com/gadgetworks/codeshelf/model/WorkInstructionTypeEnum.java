/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionTypeEnum.java,v 1.2 2013/03/17 23:10:45 jeffw Exp $
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
	ACTUAL(WorkInstructionTypeNum.ACTUAL, "ACTUAL");

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

		static final byte	INVALID	= 0;
		static final byte	PLAN	= 1;
		static final byte	ACTUAL	= 2;

		private WorkInstructionTypeNum() {
		};
	}
}
