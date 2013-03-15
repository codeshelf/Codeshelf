/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: TravelDirectionEnum.java,v 1.1 2013/03/15 14:57:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum TravelDirectionEnum {
	@EnumValue("INVALID")
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	@EnumValue("NONE")
	NONE(OrderStatusNum.NONE, "NONE"),
	@EnumValue("FORWARD")
	FORWARD(OrderStatusNum.FORWARD, "FORWARD"),
	@EnumValue("REVERSE")
	REVERSE(OrderStatusNum.REVERSE, "REVERSE"),
	@EnumValue("BOTH")
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

		static final byte	INVALID	= 0;
		static final byte	NONE	= 1;
		static final byte	FORWARD	= 2;
		static final byte	REVERSE	= 3;
		static final byte	BOTH	= 4;

		private OrderStatusNum() {
		};
	}
}
