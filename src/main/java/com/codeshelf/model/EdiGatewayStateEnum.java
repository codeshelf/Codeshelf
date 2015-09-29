/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiGatewayStateEnum.java,v 1.5 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum EdiGatewayStateEnum {
	INVALID(EdiGatewayStateType.INVALID, "INVALID"),
	UNLINKED(EdiGatewayStateType.UNLINKED, "UNLINKED"),
	LINKING(EdiGatewayStateType.LINKING, "LINKING"),
	LINKED(EdiGatewayStateType.LINKED, "LINKED"),
	LINK_FAILED(EdiGatewayStateType.LINK_FAILED, "LINK_FAILED");

	private int		mValue;
	private String	mName;

	EdiGatewayStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static EdiGatewayStateEnum getPositionTypeEnum(int inPositionTypeID) {
		EdiGatewayStateEnum result;

		switch (inPositionTypeID) {
			case EdiGatewayStateType.UNLINKED:
				result = EdiGatewayStateEnum.UNLINKED;
				break;

			case EdiGatewayStateType.LINKING:
				result = EdiGatewayStateEnum.LINKING;
				break;

			case EdiGatewayStateType.LINKED:
				result = EdiGatewayStateEnum.LINKED;
				break;

			case EdiGatewayStateType.LINK_FAILED:
				result = EdiGatewayStateEnum.LINK_FAILED;
				break;

			default:
				result = EdiGatewayStateEnum.INVALID;
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

	final static class EdiGatewayStateType {

		static final byte	INVALID		= -1;
		static final byte	UNLINKED	= 0;
		static final byte	LINKING		= 1;
		static final byte	LINKED		= 2;
		static final byte	LINK_FAILED	= 3;

		private EdiGatewayStateType() {
		};
	}
}
