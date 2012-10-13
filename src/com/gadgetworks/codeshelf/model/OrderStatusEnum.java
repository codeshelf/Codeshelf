/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderStatusEnum.java,v 1.2 2012/10/13 22:14:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NEW=NEW, RELEASED=RELEASED, INPROGRESS=INPROGRESS, COMPLETE=COMPLETE")
public enum OrderStatusEnum {
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	NEW(OrderStatusNum.NEW, "NEW"),
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
			case OrderStatusNum.NEW:
				result = OrderStatusEnum.NEW;
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
		static final byte	NEW			= 1;
		static final byte	RELEASED	= 2;
		static final byte	INPROGRESS	= 3;
		static final byte	COMPLETE	= 4;

		private OrderStatusNum() {
		};
	}
}
