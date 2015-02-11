/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PickStrategyEnum.java,v 1.3 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum OrderTypeEnum {
	// @EnumValue("INVALID")
	INVALID(OrderTypeNum.INVALID, "INVALID"),
	// Outbound customer orders picked from inventory.
	// @EnumValue("OUTBOUND")
	OUTBOUND(OrderTypeNum.OUTBOUND, "OUTBOUND"),
	// Inbound orders to put material into inventory.
	// @EnumValue("PUT")
	INBOUND(OrderTypeNum.INBOUND, "INBOUND"),
	// Orders the cross the facility to fill outbound customer orders on a CrossWall.
	// @EnumValue("CROSS")
	CROSS(OrderTypeNum.CROSS, "CROSS");

	private int		mValue;
	private String	mName;

	OrderTypeEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static OrderTypeEnum getPickStrategyEnum(int inPickStrategy) {
		OrderTypeEnum result;

		switch (inPickStrategy) {
			case OrderTypeNum.OUTBOUND:
				result = OrderTypeEnum.OUTBOUND;
				break;

			case OrderTypeNum.INBOUND:
				result = OrderTypeEnum.INBOUND;
				break;

			case OrderTypeNum.CROSS:
				result = OrderTypeEnum.CROSS;
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
		static final byte	OUTBOUND	= 0;
		static final byte	INBOUND		= 1;
		static final byte	CROSS		= 2;

		private OrderTypeNum() {
		};
	}
}
