/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentStatusEnum.java,v 1.3 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum EdiDocumentStatusEnum {
	// @EnumValue("JPEG")
	INVALID(EdiDocumentStateType.INVALID, "INVALID"),
	// @EnumValue("NEW")
	NEW(EdiDocumentStateType.NEW, "NEW"),
	// @EnumValue("SUCCESS")
	SUCCESS(EdiDocumentStateType.SUCCESS, "SUCCESS"),
	// @EnumValue("FAILED")
	FAILED(EdiDocumentStateType.FAILED, "FAILED");

	private int		mValue;
	private String	mName;

	EdiDocumentStatusEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static EdiDocumentStatusEnum getPositionTypeEnum(int inPositionTypeID) {
		EdiDocumentStatusEnum result;

		switch (inPositionTypeID) {
			case EdiDocumentStateType.NEW:
				result = EdiDocumentStatusEnum.NEW;
				break;

			case EdiDocumentStateType.SUCCESS:
				result = EdiDocumentStatusEnum.SUCCESS;
				break;

			case EdiDocumentStateType.FAILED:
				result = EdiDocumentStatusEnum.FAILED;
				break;

			default:
				result = EdiDocumentStatusEnum.INVALID;
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

	final static class EdiDocumentStateType {

		static final byte	INVALID	= -1;
		static final byte	NEW		= 0;
		static final byte	SUCCESS	= 1;
		static final byte	FAILED	= 2;

		private EdiDocumentStateType() {
		};
	}
}
