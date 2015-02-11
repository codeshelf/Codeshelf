/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2007, Jeffrey B. Williams, All rights reserved
 *  $Id: AckStateEnum.java,v 1.2 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.command;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum AckStateEnum {
	INVALID(AckStateNum.INVALID, "INVALID"),
	PENDING(AckStateNum.PENDING, "PENDING"),
	SUCCEEDED(AckStateNum.SUCCEEDED, "SUCCEEDED"),
	NO_RESPONSE(AckStateNum.NO_RESPONSE, "NORESPONSE");

	private int		mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	AckStateEnum(final int inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommandID
	 *  @return
	 */
	public static AckStateEnum getAckedStateEnum(int inNetworkTypeID) {
		AckStateEnum result = AckStateEnum.INVALID;

		switch (inNetworkTypeID) {
			case AckStateNum.PENDING:
				result = AckStateEnum.PENDING;
				break;
			case AckStateNum.SUCCEEDED:
				result = AckStateEnum.SUCCEEDED;
				break;
			case AckStateNum.NO_RESPONSE:
				result = AckStateEnum.NO_RESPONSE;
				break;
			default:
				break;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public int getValue() {
		return mValue;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getName() {
		return mName;
	}

	static final class AckStateNum {
		static final int	INVALID		= -1;
		static final int	PENDING		= 0;
		static final int	SUCCEEDED	= 1;
		static final int	NO_RESPONSE	= 2;

		private AckStateNum() {

		};
	}

}
