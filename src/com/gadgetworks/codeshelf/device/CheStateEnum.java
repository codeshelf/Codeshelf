/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CheStateEnum.java,v 1.3 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum CheStateEnum {
	// @EnumValue("INVALID")
	INVALID(CheStateNum.INVALID, "INVALID"),
	// @EnumValue("IDLE")
	IDLE(CheStateNum.IDLE, "IDLE"),
	// @EnumValue("COMPUTE_WORK")
	COMPUTE_WORK(CheStateNum.COMPUTE_WORK, "COMPUTE_WORK"),
	// @EnumValue("GET_WORK")
	GET_WORK(CheStateNum.GET_WORK, "GET_WORK"),
	// @EnumValue("LOCATION_SELECT")
	LOCATION_SELECT(CheStateNum.LOCATION_SELECT, "LOCATION_SELECT"),
	// @EnumValue("CONTAINER_SELECT")
	CONTAINER_SELECT(CheStateNum.CONTAINER_SELECT, "CONTAINER_SELECT"),
	// @EnumValue("CONTAINER_POSITION")
	CONTAINER_POSITION(CheStateNum.CONTAINER_POSITION, "CONTAINER_POSITION"),
	// @EnumValue("DO_PICK")
	DO_PICK(CheStateNum.DO_PICK, "DO_PICK"),
	// @EnumValue("SHORT_PICK")
	SHORT_PICK(CheStateNum.SHORT_PICK, "SHORT_PICK"),
	//@EnumValue("SHORT_PICK_CONFIRM")
	SHORT_PICK_CONFIRM(CheStateNum.SHORT_PICK_CONFIRM, "SHORT_PICK_CONFIRM"),
	PICK_COMPLETE(CheStateNum.PICK_COMPLETE, "PICK_COMPLETE"),
	// @EnumValue("NO_WORK")
	NO_WORK(CheStateNum.NO_WORK, "NO_WORK"),
	// @EnumValue("LOCATION_SELECT_REVIEW")
	LOCATION_SELECT_REVIEW(CheStateNum.LOCATION_SELECT_REVIEW, "LOCATION_SELECT_REVIEW"),
	// @EnumValue("CONTAINER_POSITION_INVALID")
	CONTAINER_POSITION_INVALID(CheStateNum.CONTAINER_POSITION_INVALID, "CONTAINER_POSITION_INVALID"),
	// @EnumValue("CONTAINER_SELECTION_INVALID")
	CONTAINER_SELECTION_INVALID(CheStateNum.CONTAINER_SELECTION_INVALID, "CONTAINER_SELECTION_INVALID"),
	// @EnumValue("CONTAINER_POSITION_IN_USE")
	CONTAINER_POSITION_IN_USE(CheStateNum.CONTAINER_POSITION_IN_USE, "CONTAINER_POSITION_IN_USE"),
	// @EnumValue("CLEAR_ERROR_SCAN_INVALID")
	CLEAR_ERROR_SCAN_INVALID(CheStateNum.CLEAR_ERROR_SCAN_INVALID, "CLEAR_ERROR_SCAN_INVALID");

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

			case CheStateNum.COMPUTE_WORK:
				result = CheStateEnum.COMPUTE_WORK;
				break;

			case CheStateNum.GET_WORK:
				result = CheStateEnum.GET_WORK;
				break;

			case CheStateNum.LOCATION_SELECT:
				result = CheStateEnum.LOCATION_SELECT;
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

			case CheStateNum.SHORT_PICK:
				result = CheStateEnum.SHORT_PICK;
				break;

			case CheStateNum.SHORT_PICK_CONFIRM:
				result = CheStateEnum.SHORT_PICK_CONFIRM;
				break;

			case CheStateNum.PICK_COMPLETE:
				result = CheStateEnum.PICK_COMPLETE;
				break;

			case CheStateNum.LOCATION_SELECT_REVIEW:
				result = CheStateEnum.LOCATION_SELECT_REVIEW;
				break;

			case CheStateNum.CONTAINER_POSITION_INVALID:
				result = CheStateEnum.CONTAINER_POSITION_INVALID;
				break;

			case CheStateNum.CONTAINER_SELECTION_INVALID:
				result = CheStateEnum.CONTAINER_SELECTION_INVALID;
				break;
				
			case CheStateNum.CLEAR_ERROR_SCAN_INVALID:
				result = CheStateEnum.CLEAR_ERROR_SCAN_INVALID;
				break;

			case CheStateNum.CONTAINER_POSITION_IN_USE:
				result = CheStateEnum.CONTAINER_POSITION_IN_USE;
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

		static final byte	INVALID							= -1;
		static final byte	IDLE							= 0;
		static final byte	COMPUTE_WORK					= 1;
		static final byte	GET_WORK						= 2;
		static final byte	LOCATION_SELECT					= 3;
		static final byte	CONTAINER_SELECT				= 4;
		static final byte	CONTAINER_POSITION				= 5;
		static final byte	DO_PICK							= 6;
		static final byte	SHORT_PICK						= 7;
		static final byte	PICK_COMPLETE					= 8;
		static final byte	NO_WORK							= 9;
		static final byte	SHORT_PICK_CONFIRM				= 10;	//Added Oct. 2014
		static final byte	LOCATION_SELECT_REVIEW			= 11;	//Added Dec 12 2014

		//Error States

		//Container Setup Error States
		static final byte	CONTAINER_POSITION_INVALID		= 12;	//Added Dec 29 2014
		static final byte	CONTAINER_SELECTION_INVALID		= 14;	//Addec Dec 30 2014
		static final byte	CONTAINER_POSITION_IN_USE		= 15;	//Added Dec 30 2014
		
		static final byte	CLEAR_ERROR_SCAN_INVALID	= 16;	//Added Dec 30 2014

		private CheStateNum() {
		};
	}
}
