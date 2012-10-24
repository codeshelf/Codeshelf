/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentStatusEnum.java,v 1.2 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum EdiDocumentStatusEnum {
	@EnumValue("JPEG")
	INVALID(EdiDocumentStateType.INVALID, "INVALID"),
	@EnumValue("NEW")
	NEW(EdiDocumentStateType.NEW, "NEW"),
	@EnumValue("SUCCESS")
	SUCCESS(EdiDocumentStateType.SUCCESS, "SUCCESS"),
	@EnumValue("FAILED")
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

		static final byte	INVALID	= 0;
		static final byte	NEW		= 1;
		static final byte	SUCCESS	= 2;
		static final byte	FAILED	= 3;

		private EdiDocumentStateType() {
		};
	}
}
