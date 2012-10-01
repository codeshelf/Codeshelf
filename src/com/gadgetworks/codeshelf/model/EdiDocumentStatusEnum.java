/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentStatusEnum.java,v 1.1 2012/10/01 07:16:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NEW=NEW, SUCCESS=SUCCESS, FAILED=FAILED")
public enum EdiDocumentStatusEnum {
	INVALID(EdiDocumentStateType.INVALID, "INVALID"),
	NEW(EdiDocumentStateType.NEW, "NEW"),
	SUCCESS(EdiDocumentStateType.SUCCESS, "SUCCESS"),
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
