/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProviderEnum.java,v 1.2 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, DROPBOX=DROPBOX")
public enum EdiProviderEnum {
	INVALID(EdiProviderTypeNum.INVALID, "INVALID"),
	DROPBOX(EdiProviderTypeNum.DROPBOX, "DROPBOX");

	private int		mValue;
	private String	mName;

	EdiProviderEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static EdiProviderEnum getPositionTypeEnum(int inPositionTypeID) {
		EdiProviderEnum result;

		switch (inPositionTypeID) {
			case EdiProviderTypeNum.DROPBOX:
				result = EdiProviderEnum.DROPBOX;
				break;

			default:
				result = EdiProviderEnum.INVALID;
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

	final static class EdiProviderTypeNum {

		static final byte	INVALID	= 0;
		static final byte	DROPBOX	= 1;

		private EdiProviderTypeNum() {
		};
	}
}
