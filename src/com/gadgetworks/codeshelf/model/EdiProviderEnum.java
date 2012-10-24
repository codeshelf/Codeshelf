/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProviderEnum.java,v 1.3 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum EdiProviderEnum {
	@EnumValue("INVALID")
	INVALID(EdiProviderTypeNum.INVALID, "INVALID"),
	@EnumValue("DROPBOX")
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
