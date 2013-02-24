/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CheStateEnum.java,v 1.1 2013/02/24 22:54:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import com.avaje.ebean.annotation.EnumValue;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum CheStateEnum {
	@EnumValue("INVALID")
	INVALID(CheStateNum.INVALID, "INVALID"),
	@EnumValue("IDLE")
	IDLE(CheStateNum.IDLE, "IDLE"),
	@EnumValue("LOCATION_SETUP")
	LOCATION_SETUP(CheStateNum.LOCATION_SETUP, "LOCATION_SETUP"),
	@EnumValue("CONTAINER_SELECT")
	CONTAINER_SELECT(CheStateNum.CONTAINER_SELECT, "CONTAINER_SELECT"),
	@EnumValue("CONTAINER_POSITION")
	CONTAINER_POSITION(CheStateNum.CONTAINER_POSITION, "CONTAINER_POSITION"),
	@EnumValue("DO_PICK")
	DO_PICK(CheStateNum.DO_PICK, "DO_PICK"),
	@EnumValue("PICK_COMPLETE")
	PICK_COMPLETE(CheStateNum.PICK_COMPLETE, "PICK_COMPLETE");

	private int		mValue;
	private String	mName;

	CheStateEnum(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static CheStateEnum geControlProtocolEnum(int inProtocolNum) {
		CheStateEnum result;

		switch (inProtocolNum) {
			case CheStateNum.IDLE:
				result = CheStateEnum.IDLE;
				break;

			case CheStateNum.LOCATION_SETUP:
				result = CheStateEnum.LOCATION_SETUP;
				break;

			case CheStateNum.CONTAINER_SELECT:
				result = CheStateEnum.CONTAINER_SELECT;
				break;

			case CheStateNum.CONTAINER_POSITION:
				result = CheStateEnum.CONTAINER_POSITION;
				break;

			case CheStateNum.DO_PICK:
				result = CheStateEnum.DO_PICK;
				break;

			case CheStateNum.PICK_COMPLETE:
				result = CheStateEnum.PICK_COMPLETE;
				break;

			default:
				result = CheStateEnum.INVALID;
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

	final static class CheStateNum {

		static final byte	INVALID				= 0;
		static final byte	IDLE				= 1;
		static final byte	LOCATION_SETUP		= 2;
		static final byte	CONTAINER_SELECT	= 3;
		static final byte	CONTAINER_POSITION	= 4;
		static final byte	DO_PICK				= 5;
		static final byte	PICK_COMPLETE			= 6;

		private CheStateNum() {
		};
	}
}
