/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseTypeEnum.java,v 1.3 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *
 *	ACTOR_DESCRIPTOR responds to a request for a description of the remote actor:
 *		name
 *		desc
 *		KVP/REP count (0..N)
 *		endpoint count (0..N)
 *
 *	ACTOR_KVP_DESCRIPTOR responds to a request for the KVP/REP pair #N for the actor:
 *		Key
 *		Value
 *		
 */

public enum ResponseTypeEnum {

	INVALID(ResponseNum.INVALID, "INVALID"),
	ACTOR_DESCRIPTOR(ResponseNum.ACTOR_DESCRIPTOR, "ACTOR_DESC)"),
	ACTOR_KVP(ResponseNum.ACTOR_KVP, "ACTOR_KVP"),
	;

	private byte	mValue;
	private String	mName;

	ResponseTypeEnum(final byte inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static ResponseTypeEnum getResponseTypeEnum(int inResponseTypeID) {
		ResponseTypeEnum result = ResponseTypeEnum.INVALID;

		switch (inResponseTypeID) {
			case ResponseNum.ACTOR_DESCRIPTOR:
				result = ResponseTypeEnum.ACTOR_DESCRIPTOR;
				break;

			case ResponseNum.ACTOR_KVP:
				result = ResponseTypeEnum.ACTOR_KVP;
				break;

			default:
				result = ResponseTypeEnum.INVALID;
				break;
		}

		return result;
	}

	public byte getValue() {
		return mValue;
	}

	public String getName() {
		return mName;
	}

	static final class ResponseNum {
		static final byte	INVALID				= 0;
		static final byte	ACTOR_DESCRIPTOR	= 1;
		static final byte	ACTOR_KVP			= 2;
		
		private ResponseNum() {
			
		}
	}

}
