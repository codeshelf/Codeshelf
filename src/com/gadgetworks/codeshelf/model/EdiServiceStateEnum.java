/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceStateEnum.java,v 1.5 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum EdiServiceStateEnum {
	@EnumValue("INVALID")
	INVALID(EdiServiceStateType.INVALID, "INVALID"),
	@EnumValue("UNLINKED")
	UNLINKED(EdiServiceStateType.UNLINKED, "UNLINKED"),
	@EnumValue("LINKING")
	LINKING(EdiServiceStateType.LINKING, "LINKING"),
	@EnumValue("LINKED")
	LINKED(EdiServiceStateType.LINKED, "LINKED"),
	@EnumValue("LINK_FAILED")
	LINK_FAILED(EdiServiceStateType.LINK_FAILED, "LINK_FAILED");

	private int		mValue;
	private String	mName;

	EdiServiceStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static EdiServiceStateEnum getPositionTypeEnum(int inPositionTypeID) {
		EdiServiceStateEnum result;

		switch (inPositionTypeID) {
			case EdiServiceStateType.UNLINKED:
				result = EdiServiceStateEnum.UNLINKED;
				break;

			case EdiServiceStateType.LINKING:
				result = EdiServiceStateEnum.LINKING;
				break;

			case EdiServiceStateType.LINKED:
				result = EdiServiceStateEnum.LINKED;
				break;

			case EdiServiceStateType.LINK_FAILED:
				result = EdiServiceStateEnum.LINK_FAILED;
				break;

			default:
				result = EdiServiceStateEnum.INVALID;
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

	final static class EdiServiceStateType {

		static final byte	INVALID		= -1;
		static final byte	UNLINKED	= 0;
		static final byte	LINKING		= 1;
		static final byte	LINKED		= 2;
		static final byte	LINK_FAILED	= 3;

		private EdiServiceStateType() {
		};
	}
}
