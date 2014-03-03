/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PickStrategyEnum.java,v 1.3 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum OrderTypeEnum {
	@EnumValue("INVALID")
	INVALID(OrderTypeNum.INVALID, "INVALID"),
	@EnumValue("PICK")
	PICK(OrderTypeNum.PICK, "PICK"),
	@EnumValue("PUT")
	PUT(OrderTypeNum.PUT, "PUT"),
	@EnumValue("WONDERWALL")
	WONDERWALL(OrderTypeNum.WONDERWALL, "WONDERWALL");

	private int		mValue;
	private String	mName;

	OrderTypeEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static OrderTypeEnum getPickStrategyEnum(int inPickStrategy) {
		OrderTypeEnum result;

		switch (inPickStrategy) {
			case OrderTypeNum.PICK:
				result = OrderTypeEnum.PICK;
				break;

			case OrderTypeNum.PUT:
				result = OrderTypeEnum.PUT;
				break;

			case OrderTypeNum.WONDERWALL:
				result = OrderTypeEnum.WONDERWALL;
				break;

			default:
				result = OrderTypeEnum.INVALID;
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

	static final class OrderTypeNum {

		static final byte	INVALID		= -1;
		static final byte	PICK		= 0;
		static final byte	PUT			= 1;
		static final byte	WONDERWALL	= 2;

		private OrderTypeNum() {
		};
	}
}
