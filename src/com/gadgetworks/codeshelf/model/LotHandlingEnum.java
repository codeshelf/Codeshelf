/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LotHandlingEnum.java,v 1.1 2012/10/21 02:02:18 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NONE=NONE, FIFO=FIFO, REQUIRED=REQUIRED")
public enum LotHandlingEnum {
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	NONE(OrderStatusNum.NONE, "NONE"),
	FIFO(OrderStatusNum.FIFO, "FIFO"),
	REQUIRED(OrderStatusNum.REQUIRED, "REQUIRED");

	private int		mValue;
	private String	mName;

	LotHandlingEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static LotHandlingEnum getLotHandlingEnum(int inLotHandling) {
		LotHandlingEnum result;

		switch (inLotHandling) {
			case OrderStatusNum.NONE:
				result = LotHandlingEnum.NONE;
				break;

			case OrderStatusNum.FIFO:
				result = LotHandlingEnum.FIFO;
				break;

			case OrderStatusNum.REQUIRED:
				result = LotHandlingEnum.REQUIRED;
				break;

			default:
				result = LotHandlingEnum.INVALID;
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

	static final class OrderStatusNum {

		static final byte	INVALID		= 0;
		static final byte	NONE		= 1;
		static final byte	FIFO		= 2;
		static final byte	REQUIRED	= 3;

		private OrderStatusNum() {
		};
	}
}
