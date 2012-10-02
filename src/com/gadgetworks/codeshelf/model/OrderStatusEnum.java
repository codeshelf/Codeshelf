/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderStatusEnum.java,v 1.1 2012/10/02 15:12:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.avaje.ebean.annotation.EnumMapping;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
@EnumMapping(nameValuePairs = "INVALID=INVALID, NEW=NEW, INPROGRESS=INPROGRESS, COMPLETE=COMPLETE")
public enum OrderStatusEnum {
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	NEW(OrderStatusNum.NEW, "NEW"),
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
		static final byte	INPROGRESS	= 2;
		static final byte	COMPLETE	= 3;

		private OrderStatusNum() {
		};
	}
}
