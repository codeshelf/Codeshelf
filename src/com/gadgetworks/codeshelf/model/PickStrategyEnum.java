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
public enum PickStrategyEnum {
	@EnumValue("INVALID")
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	@EnumValue("SERIAL")
	SERIAL(OrderStatusNum.SERIAL, "SERIAL"),
	@EnumValue("PARALLEL")
	PARALLEL(OrderStatusNum.PARALLEL, "PARALLEL");

	private int		mValue;
	private String	mName;

	PickStrategyEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static PickStrategyEnum getPickStrategyEnum(int inPickStrategy) {
		PickStrategyEnum result;

		switch (inPickStrategy) {
			case OrderStatusNum.SERIAL:
				result = PickStrategyEnum.SERIAL;
				break;

			case OrderStatusNum.PARALLEL:
				result = PickStrategyEnum.PARALLEL;
				break;

			default:
				result = PickStrategyEnum.INVALID;
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
		static final byte	SERIAL		= 0;
		static final byte	PARALLEL	= 1;

		private OrderStatusNum() {
		};
	}
}
