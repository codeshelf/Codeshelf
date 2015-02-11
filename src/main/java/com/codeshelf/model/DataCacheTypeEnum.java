/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DataCacheTypeEnum.java,v 1.5 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum DataCacheTypeEnum {
	// @EnumValue("INVALID")
	INVALID(DataCacheTypeNum.INVALID, "INVALID"),
	// @EnumValue("JPEG")
	JPEG(DataCacheTypeNum.JPEG, "JPEG");

	private int		mValue;
	private String	mName;

	DataCacheTypeEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static DataCacheTypeEnum getOnlineStatusEnum(int inRemoteDataTypeID) {
		DataCacheTypeEnum result;

		switch (inRemoteDataTypeID) {
			case DataCacheTypeNum.JPEG:
				result = DataCacheTypeEnum.JPEG;
				break;

			default:
				result = DataCacheTypeEnum.INVALID;
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

	static final class DataCacheTypeNum {

		static final byte	INVALID	= -1;
		static final byte	JPEG	= 0;

		private DataCacheTypeNum() {
		};
	}
}
