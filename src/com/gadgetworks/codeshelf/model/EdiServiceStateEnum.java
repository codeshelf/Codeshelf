/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiServiceStateEnum.java,v 1.1 2012/09/06 22:59:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, UNREGISTERED=UNREGISTERED, REGISTERED=REGISTERED, FAILED=FAILED")
public enum EdiServiceStateEnum {
	INVALID(EdiServiceStateType.INVALID, "INVALID"),
	UNREGISTERED(EdiServiceStateType.UNREGISTERED, "UNREGISTERED"),
	REGISTERED(EdiServiceStateType.REGISTERED, "REGISTERED"),
	FAILED(EdiServiceStateType.FAILED, "FAILED");

	private int		mValue;
	private String	mName;

	EdiServiceStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static EdiServiceStateEnum getPositionTypeEnum(int inPositionTypeID) {
		EdiServiceStateEnum result;

		switch (inPositionTypeID) {
			case EdiServiceStateType.UNREGISTERED:
				result = EdiServiceStateEnum.UNREGISTERED;
				break;

			case EdiServiceStateType.REGISTERED:
				result = EdiServiceStateEnum.REGISTERED;
				break;

			case EdiServiceStateType.FAILED:
				result = EdiServiceStateEnum.FAILED;
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

		static final byte	INVALID			= 0;
		static final byte	UNREGISTERED	= 1;
		static final byte	REGISTERED		= 2;
		static final byte	FAILED			= 3;

		private EdiServiceStateType() {
		};
	}
}
