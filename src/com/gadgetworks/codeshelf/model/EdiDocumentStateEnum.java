/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiDocumentStateEnum.java,v 1.2 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NEW=NEW, SUCCESS=SUCCESS, FAILED=FAILED")
public enum EdiDocumentStateEnum {
	INVALID(EdiDocumentStateType.INVALID, "INVALID"),
	NEW(EdiDocumentStateType.NEW, "NEW"),
	SUCCESS(EdiDocumentStateType.SUCCESS, "SUCCESS"),
	FAILED(EdiDocumentStateType.FAILED, "FAILED");

	private int		mValue;
	private String	mName;

	EdiDocumentStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static EdiDocumentStateEnum getPositionTypeEnum(int inPositionTypeID) {
		EdiDocumentStateEnum result;

		switch (inPositionTypeID) {
			case EdiDocumentStateType.NEW:
				result = EdiDocumentStateEnum.NEW;
				break;

			case EdiDocumentStateType.SUCCESS:
				result = EdiDocumentStateEnum.SUCCESS;
				break;

			case EdiDocumentStateType.FAILED:
				result = EdiDocumentStateEnum.FAILED;
				break;

			default:
				result = EdiDocumentStateEnum.INVALID;
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
