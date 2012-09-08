/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: QueryTypeEnum.java,v 1.3 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;

// --------------------------------------------------------------------------
/**
 *  These are the enumerateds for the toy manager query.
 *  
 *  TOY MANAGEMENT QUERY TYPES
 *
 *	ACTOR_DESCRIPTOR ask for a description of the remote actor:
 *		name
 *		desc
 *		KVP/REP count (0..N)
 *		endpoint count (0..N)
 *
 *	ACTOR_KVP_DESCRIPTOR asks for the KVP/REP pair #N for the actor:
 *		Key
 *		Value
 *		
 *  @author jeffw
 *		
 */

public enum QueryTypeEnum {

	INVALID(QueryNum.INVALID, "INVALID"),

	// Enumerations for the toy network.
	ACTOR_DESCRIPTOR(QueryNum.ACTOR_DESCRIPTOR, "ACTOR_DESC"),
	ACTOR_KVP(QueryNum.ACTOR_KVP, "ACTOR_KVP");

	private byte	mValue;
	private String	mName;

	QueryTypeEnum(final byte inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static QueryTypeEnum getQueryTypeEnum(int inQueryTypeID) {
		QueryTypeEnum result = QueryTypeEnum.INVALID;

		switch (inQueryTypeID) {
			case QueryNum.ACTOR_DESCRIPTOR:
				result = QueryTypeEnum.ACTOR_DESCRIPTOR;
				break;

			case QueryNum.ACTOR_KVP:
				result = QueryTypeEnum.ACTOR_KVP;
				break;

			default:
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

	static final class QueryNum {

		static final byte	INVALID				= 0;
		static final byte	ACTOR_DESCRIPTOR	= 1;
		static final byte	ACTOR_KVP			= 2;

		private QueryNum() {
		};
	}
}
