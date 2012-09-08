/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DataCacheTypeEnum.java,v 1.3 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, JPEG=JPEG")
public enum DataCacheTypeEnum {
	INVALID(DataCacheTypeNum.INVALID, "INVALID"),
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

		static final byte	INVALID	= 0;
		static final byte	JPEG	= 1;

		private DataCacheTypeNum() {
		};
	}
}
