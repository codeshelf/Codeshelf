/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: TravelDirectionEnum.java,v 1.2 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum TravelDirectionEnum {
	// @EnumValue("INVALID")
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	// @EnumValue("NONE")
	NONE(OrderStatusNum.NONE, "NONE"),
	// @EnumValue("FORWARD")
	FORWARD(OrderStatusNum.FORWARD, "FORWARD"),
	// @EnumValue("REVERSE")
	REVERSE(OrderStatusNum.REVERSE, "REVERSE"),
	// @EnumValue("BOTH")
	BOTH(OrderStatusNum.BOTH, "BOTH");

	private int		mValue;
	private String	mName;

	TravelDirectionEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static TravelDirectionEnum getPickStrategyEnum(int inPickStrategy) {
		TravelDirectionEnum result;

		switch (inPickStrategy) {
			case OrderStatusNum.NONE:
				result = TravelDirectionEnum.NONE;
				break;

			case OrderStatusNum.FORWARD:
				result = TravelDirectionEnum.FORWARD;
				break;

			case OrderStatusNum.REVERSE:
				result = TravelDirectionEnum.REVERSE;
				break;

			case OrderStatusNum.BOTH:
				result = TravelDirectionEnum.BOTH;
				break;

			default:
				result = TravelDirectionEnum.INVALID;
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

		static final byte	INVALID	= 1;
		static final byte	NONE	= 0;
		static final byte	FORWARD	= 1;
		static final byte	REVERSE	= 2;
		static final byte	BOTH	= 3;

		private OrderStatusNum() {
		};
	}
}
