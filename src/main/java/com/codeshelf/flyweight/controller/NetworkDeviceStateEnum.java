/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetworkDeviceStateEnum.java,v 1.2 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.controller;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum NetworkDeviceStateEnum {
	INVALID(NetworkDeviceStateNum.INVALID, "INVALID"),
	WAKE_RCVD(NetworkDeviceStateNum.WAKE_RCVD, "WAKE_RCVD"),
	ASSIGN_SENT(NetworkDeviceStateNum.ASSIGN_SENT, "ASSIGN_SENT"),
	ASSIGNACK_RCVD(NetworkDeviceStateNum.ASSIGNACK_RCVD, "ASSIGNACK_RCVD"),
	SETUP(NetworkDeviceStateNum.SETUP, "SETUP"),
	STARTED(NetworkDeviceStateNum.STARTED, "STARTED"),
	LOST(NetworkDeviceStateNum.LOST, "LOST"),
	STOPPED(NetworkDeviceStateNum.STOPPED, "STOPPED"),
	TERMINATED(NetworkDeviceStateNum.TERMINATED, "TERMINATED");

	private int		mValue;
	private String	mName;

	NetworkDeviceStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static NetworkDeviceStateEnum getNetworkDeviceStateEnum(int inQueryTypeID) {
		NetworkDeviceStateEnum result;

		switch (inQueryTypeID) {
			case NetworkDeviceStateNum.WAKE_RCVD:
				result = NetworkDeviceStateEnum.WAKE_RCVD;
				break;

			case NetworkDeviceStateNum.ASSIGN_SENT:
				result = NetworkDeviceStateEnum.ASSIGN_SENT;
				break;

			case NetworkDeviceStateNum.ASSIGNACK_RCVD:
				result = NetworkDeviceStateEnum.ASSIGNACK_RCVD;
				break;

			case NetworkDeviceStateNum.SETUP:
				result = NetworkDeviceStateEnum.SETUP;
				break;

			case NetworkDeviceStateNum.STARTED:
				result = NetworkDeviceStateEnum.STARTED;
				break;

			case NetworkDeviceStateNum.LOST:
				result = NetworkDeviceStateEnum.LOST;
				break;

			case NetworkDeviceStateNum.STOPPED:
				result = NetworkDeviceStateEnum.STOPPED;
				break;

			case NetworkDeviceStateNum.TERMINATED:
				result = NetworkDeviceStateEnum.TERMINATED;
				break;

			default:
				result = NetworkDeviceStateEnum.INVALID;
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

	final static class NetworkDeviceStateNum {
		static final byte	INVALID			= -1;
		static final byte	WAKE_RCVD		= 0;
		static final byte	ASSIGN_SENT		= 1;
		static final byte	ASSIGNACK_RCVD	= 2;
		static final byte	SETUP			= 3;
		static final byte	STARTED			= 4;
		static final byte	LOST			= 5;
		static final byte	STOPPED			= 6;
		static final byte	TERMINATED		= 7;

		private NetworkDeviceStateNum() {

		}
	}
}
