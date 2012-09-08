/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: AckStateEnum.java,v 1.3 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

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
	 *  @param inAckValue
	 *  @param inName
	 */
	AckStateEnum(final int inAckValue, final String inName) {
		mValue = inAckValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommandID
	 *  @return
	 */
	public static AckStateEnum getAckedStateEnum(int inAckState) {
		AckStateEnum result = AckStateEnum.INVALID;

		switch (inAckState) {
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
		static final int	INVALID		= 0;
		static final int	PENDING		= 1;
		static final int	SUCCEEDED	= 2;
		static final int	NO_RESPONSE	= 3;

		private AckStateNum() {

		};
	}

}
