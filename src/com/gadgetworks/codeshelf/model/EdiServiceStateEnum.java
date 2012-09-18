/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceStateEnum.java,v 1.3 2012/09/18 14:47:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, UNLINKED=UNLINKED, LINKING=LINKING, LINKED=LINKED, LINK_FAILED=LINK_FAILED")
public enum EdiServiceStateEnum {
	INVALID(EdiServiceStateType.INVALID, "INVALID"),
	UNLINKED(EdiServiceStateType.UNLINKED, "UNLINKED"),
	LINKING(EdiServiceStateType.LINKING, "LINKING"),
	LINKED(EdiServiceStateType.LINKED, "LINKED"),
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

		static final byte	INVALID		= 0;
		static final byte	UNLINKED	= 1;
		static final byte	LINKING		= 2;
		static final byte	LINKED		= 3;
		static final byte	LINK_FAILED	= 4;

		private EdiServiceStateType() {
		};
	}
}
