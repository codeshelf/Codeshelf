/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderStatusEnum.java,v 1.3 2012/10/14 01:05:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, CREATED=CREATED, RELEASED=RELEASED, INPROGRESS=INPROGRESS, COMPLETE=COMPLETE")
public enum OrderStatusEnum {
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	CREATED(OrderStatusNum.CREATED, "CREATED"),
	RELEASED(OrderStatusNum.RELEASED, "RELEASED"),
	INPROGRESS(OrderStatusNum.INPROGRESS, "INPROGRESS"),
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
