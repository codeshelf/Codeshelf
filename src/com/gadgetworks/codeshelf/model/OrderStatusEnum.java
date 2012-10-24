/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderStatusEnum.java,v 1.4 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum OrderStatusEnum {
	@EnumValue("INVALID")
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	@EnumValue("CREATED")
	CREATED(OrderStatusNum.CREATED, "CREATED"),
	@EnumValue("RELEASED")
	RELEASED(OrderStatusNum.RELEASED, "RELEASED"),
	@EnumValue("INPROGRESS")
	INPROGRESS(OrderStatusNum.INPROGRESS, "INPROGRESS"),
	@EnumValue("COMPLETE")
	COMPLETE(OrderStatusNum.COMPLETE, "COMPLETE");

	private int		mValue;
	private String	mName;

	OrderStatusEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static OrderStatusEnum getOrderStatusEnum(int inOnlineStatusID) {
		OrderStatusEnum result;

		switch (inOnlineStatusID) {
			case OrderStatusNum.CREATED:
				result = OrderStatusEnum.CREATED;
				break;

			case OrderStatusNum.RELEASED:
				result = OrderStatusEnum.RELEASED;
				break;

			case OrderStatusNum.INPROGRESS:
				result = OrderStatusEnum.INPROGRESS;
				break;

			case OrderStatusNum.COMPLETE:
				result = OrderStatusEnum.COMPLETE;
				break;

			default:
				result = OrderStatusEnum.INVALID;
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
		static final byte	CREATED			= 1;
		static final byte	RELEASED	= 2;
		static final byte	INPROGRESS	= 3;
		static final byte	COMPLETE	= 4;

		private OrderStatusNum() {
		};
	}
}
