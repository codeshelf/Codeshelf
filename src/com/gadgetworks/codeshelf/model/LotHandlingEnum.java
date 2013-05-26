/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LotHandlingEnum.java,v 1.3 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum LotHandlingEnum {
	@EnumValue("INVALID")
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	@EnumValue("NONE")
	NONE(OrderStatusNum.NONE, "NONE"),
	@EnumValue("FIFO")
	FIFO(OrderStatusNum.FIFO, "FIFO"),
	@EnumValue("REQUIRED")
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

		static final byte	INVALID		= -1;
		static final byte	NONE		= 0;
		static final byte	FIFO		= 1;
		static final byte	REQUIRED	= 2;

		private OrderStatusNum() {
		};
	}
}
