/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderStatusEnum.java,v 1.6 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum OrderStatusEnum {
	// @EnumValue("INVALID")
	INVALID(OrderStatusNum.INVALID, "INVALID"),
	// @EnumValue("CREATED")
	CREATED(OrderStatusNum.CREATED, "CREATED"),
	// @EnumValue("RELEASE")
	RELEASE(OrderStatusNum.RELEASE, "RELEASE"),
	// @EnumValue("INPROGRESS")
	INPROGRESS(OrderStatusNum.INPROGRESS, "INPROGRESS"),
	// @EnumValue("COMPLETE")
	COMPLETE(OrderStatusNum.COMPLETE, "COMPLETE"),
	// @EnumValue("SHORT")
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

		static final byte	INVALID		= -1;
		static final byte	CREATED		= 0;
		static final byte	RELEASE		= 1;
		static final byte	INPROGRESS	= 2;
		static final byte	COMPLETE	= 3;
		static final byte	SHORT		= 4;

		private OrderStatusNum() {
		};
	}
}
