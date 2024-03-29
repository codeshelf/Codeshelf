/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstructionTypeEnum.java,v 1.3 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

import java.util.List;

import com.google.common.collect.ImmutableList;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum WorkInstructionTypeEnum {
	// @EnumValue("INVALID")
	INVALID(WorkInstructionTypeNum.INVALID, "INVALID", false),
	// @EnumValue("PLAN")
	PLAN(WorkInstructionTypeNum.PLAN, "PLAN", false),
	// @EnumValue("ACTUAL")
	ACTUAL(WorkInstructionTypeNum.ACTUAL, "ACTUAL", false),
	// @EnumValue("INDICATOR")
	INDICATOR(WorkInstructionTypeNum.INDICATOR, "INDICATOR", false),
	// @EnumValue("HK_REPEATPOS")
	HK_REPEATPOS(WorkInstructionTypeNum.HK_REPEATPOS, "HK_REPEATPOS", true),
	// @EnumValue("HK_BAYCOMPLETE")
	HK_BAYCOMPLETE(WorkInstructionTypeNum.HK_BAYCOMPLETE, "HK_BAYCOMPLETE", true);
	
	// If you add a new one, please search code for filters like this
	// 		String filter = "(assignedChe.persistentId = :chePersistentId) and (typeEnum = :typeplan or typeEnum = :typehkbaychange or typeEnum = :typehkrepeat) and (posAlongPath >= :pos)";


	private int		mValue;
	private String	mName;
	private boolean mHousekeeping;

	WorkInstructionTypeEnum(final int inValue, final String inName, boolean isHousekeeping) {
		mValue = inValue;
		mName = inName;
		mHousekeeping = isHousekeeping;
	}
	
	public static List<WorkInstructionTypeEnum> getHousekeepingTypeEnums() {
		return ImmutableList.of(HK_REPEATPOS, HK_BAYCOMPLETE);
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

			case WorkInstructionTypeNum.HK_REPEATPOS:
				result = WorkInstructionTypeEnum.HK_REPEATPOS;
				break;

			case WorkInstructionTypeNum.HK_BAYCOMPLETE:
				result = WorkInstructionTypeEnum.HK_BAYCOMPLETE;
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

	public boolean isHousekeeping() {
		return mHousekeeping;
	}

	static final class WorkInstructionTypeNum {

		static final byte	INVALID		= -1;
		static final byte	PLAN		= 0;
		static final byte	ACTUAL		= 1;
		static final byte	INDICATOR	= 2;
		static final byte	HK_REPEATPOS	= 3;
		static final byte	HK_BAYCOMPLETE	= 4;

		private WorkInstructionTypeNum() {
		};
	}

}
