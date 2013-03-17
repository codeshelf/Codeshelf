/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderStatusEnum.java,v 1.5 2013/03/17 23:10:45 jeffw Exp $
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
	@EnumValue("RELEASE")
	RELEASE(OrderStatusNum.RELEASE, "RELEASE"),
	@EnumValue("INPROGRESS")
	INPROGRESS(OrderStatusNum.INPROGRESS, "INPROGRESS"),
	@EnumValue("COMPLETE")
	COMPLETE(OrderStatusNum.COMPLETE, "COMPLETE"),
	@EnumValue("SHORT")
	SHORT(OrderStatusNum.SHORT, "SHORT");

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

			case OrderStatusNum.RELEASE:
				result = OrderStatusEnum.RELEASE;
				break;

			case OrderStatusNum.INPROGRESS:
				result = OrderStatusEnum.INPROGRESS;
				break;

			case OrderStatusNum.COMPLETE:
				result = OrderStatusEnum.COMPLETE;
				break;

			case OrderStatusNum.SHORT:
				result = OrderStatusEnum.SHORT;
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
		static final byte	CREATED		= 1;
		static final byte	RELEASE		= 2;
		static final byte	INPROGRESS	= 3;
		static final byte	COMPLETE	= 4;
		static final byte	SHORT		= 5;

		private OrderStatusNum() {
		};
	}
}
