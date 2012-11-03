/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathDirectionEnum.java,v 1.1 2012/11/03 23:57:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum PathDirectionEnum {
	@EnumValue("INVALID")
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	@EnumValue("NONE")
	NONE(OrderStatusNum.NONE, "NONE"),
	@EnumValue("HEAD")
	HEAD(OrderStatusNum.HEAD, "HEAD"),
	@EnumValue("TAIL")
	TAIL(OrderStatusNum.TAIL, "TAIL"),
	@EnumValue("BOTH")
	BOTH(OrderStatusNum.BOTH, "BOTH");

	private int		mValue;
	private String	mName;

	PathDirectionEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static PathDirectionEnum getPickStrategyEnum(int inPickStrategy) {
		PathDirectionEnum result;

		switch (inPickStrategy) {
			case OrderStatusNum.NONE:
				result = PathDirectionEnum.NONE;
				break;

			case OrderStatusNum.HEAD:
				result = PathDirectionEnum.HEAD;
				break;

			case OrderStatusNum.TAIL:
				result = PathDirectionEnum.TAIL;
				break;

			case OrderStatusNum.BOTH:
				result = PathDirectionEnum.BOTH;
				break;

			default:
				result = PathDirectionEnum.INVALID;
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
		static final byte	HEAD	= 2;
		static final byte	TAIL	= 3;
		static final byte	BOTH	= 4;

		private OrderStatusNum() {
		};
	}
}
