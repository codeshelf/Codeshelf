/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProviderEnum.java,v 1.4 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum EdiProviderEnum {
	// @EnumValue("INVALID")
	INVALID(EdiProviderTypeNum.INVALID, "INVALID"),
	// @EnumValue("DROPBOX")
	DROPBOX(EdiProviderTypeNum.DROPBOX, "DROPBOX"),
	// @EnumValue("IRONMQ")
	IRONMQ(EdiProviderTypeNum.IRONMQ, "IRONMQ"),
	// 
	OTHER(EdiProviderTypeNum.OTHER, "OTHER");

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

			case EdiProviderTypeNum.IRONMQ:
				result = EdiProviderEnum.IRONMQ;
				break;
				
			case EdiProviderTypeNum.OTHER:
				result = EdiProviderEnum.OTHER;
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

		static final byte	INVALID	= -1;
		static final byte	DROPBOX	= 0;
		static final byte	IRONMQ	= 1;
		static final byte	OTHER	= 2;

		private EdiProviderTypeNum() {
		};
	}
}
