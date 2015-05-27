/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CheStateEnum.java,v 1.3 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum CheStateEnum {
	INVALID(CheStateNum.INVALID, "INVALID"),
	IDLE(CheStateNum.IDLE, "IDLE"),
	VERIFYING_BADGE(CheStateNum.VERIFYING_BADGE, "VERIFYING_BADGE"),
	COMPUTE_WORK(CheStateNum.COMPUTE_WORK, "COMPUTE_WORK"),
	GET_WORK(CheStateNum.GET_WORK, "GET_WORK"),
	LOCATION_SELECT(CheStateNum.LOCATION_SELECT, "LOCATION_SELECT"),
	CONTAINER_SELECT(CheStateNum.CONTAINER_SELECT, "CONTAINER_SELECT"),
	CONTAINER_POSITION(CheStateNum.CONTAINER_POSITION, "CONTAINER_POSITION"),
	DO_PICK(CheStateNum.DO_PICK, "DO_PICK"),
	SHORT_PICK(CheStateNum.SHORT_PICK, "SHORT_PICK"),
	SHORT_PICK_CONFIRM(CheStateNum.SHORT_PICK_CONFIRM, "SHORT_PICK_CONFIRM"),
	PICK_COMPLETE(CheStateNum.PICK_COMPLETE, "PICK_COMPLETE"),
	PICK_COMPLETE_CURR_PATH(CheStateNum.PICK_COMPLETE_CURR_PATH, "PICK_COMPLETE_CURR_PATH"),
	NO_WORK(CheStateNum.NO_WORK, "NO_WORK"),
	LOCATION_SELECT_REVIEW(CheStateNum.LOCATION_SELECT_REVIEW, "LOCATION_SELECT_REVIEW"),
	CONTAINER_POSITION_INVALID(CheStateNum.CONTAINER_POSITION_INVALID, "CONTAINER_POSITION_INVALID"),
	CONTAINER_SELECTION_INVALID(CheStateNum.CONTAINER_SELECTION_INVALID, "CONTAINER_SELECTION_INVALID"),
	CONTAINER_POSITION_IN_USE(CheStateNum.CONTAINER_POSITION_IN_USE, "CONTAINER_POSITION_IN_USE"),
	NO_CONTAINERS_SETUP(CheStateNum.NO_CONTAINERS_SETUP, "NO_CONTAINERS_SETUP"),

	// new states only used in Line_Scan mode. Many are used in both line_scan and Setup_Orders, so keeping all together.
	READY(CheStateNum.READY, "READY"),
	ABANDON_CHECK(CheStateNum.ABANDON_CHECK, "ABANDON_CHECK"),

	// states used in Setup_Orders mode if SCANPICK parameter is set.
	SCAN_SOMETHING(CheStateNum.SCAN_SOMETHING, "SCAN_SOMETHING"),
	SCAN_SOMETHING_SHORT(CheStateNum.SCAN_SOMETHING_SHORT, "SCAN_SOMETHING_SHORT"),

	// states used for inventory scan DEV-644
	SCAN_GTIN(CheStateNum.SCAN_GTIN, "SCAN_GTIN"),

	// states used for put wall DEV-708
	PUT_WALL_SCAN_LOCATION(CheStateNum.PUT_WALL_SCAN_LOCATION, "PUT_WALL_SCAN_LOCATION"),
	PUT_WALL_SCAN_ORDER(CheStateNum.PUT_WALL_SCAN_ORDER, "PUT_WALL_SCAN_ORDER"),

	// states used for put wall DEV-712, DEV-713
	PUT_WALL_SCAN_WALL(CheStateNum.PUT_WALL_SCAN_WALL, "PUT_WALL_SCAN_WALL"),
	PUT_WALL_SCAN_ITEM(CheStateNum.PUT_WALL_SCAN_ITEM, "PUT_WALL_SCAN_ITEM"),
	DO_PUT(CheStateNum.DO_PUT, "DO_PUT"),
	GET_PUT_INSTRUCTION(CheStateNum.GET_PUT_INSTRUCTION, "GET_PUT_INSTRUCTION"),
	NO_PUT_WORK(CheStateNum.NO_PUT_WORK, "NO_PUT_WORK"),
	SHORT_PUT(CheStateNum.SHORT_PUT, "SHORT_PUT"),
	SHORT_PUT_CONFIRM(CheStateNum.SHORT_PUT_CONFIRM, "SHORT_PUT_CONFIRM"),

	// state for Che setup persistence
	SETUP_SUMMARY(CheStateNum.SETUP_SUMMARY, "SETUP_SUMMARY"),

	// states used for mobile CHE association
	REMOTE(CheStateNum.REMOTE, "REMOTE"),
	REMOTE_PENDING(CheStateNum.REMOTE_PENDING, "REMOTE_PENDING"),
	REMOTE_LINKED(CheStateNum.REMOTE_LINKED, "REMOTE_LINKED");

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

			case CheStateNum.VERIFYING_BADGE:
				result = CheStateEnum.VERIFYING_BADGE;
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

			case CheStateNum.CONTAINER_POSITION_IN_USE:
				result = CheStateEnum.CONTAINER_POSITION_IN_USE;
				break;

			case CheStateNum.NO_CONTAINERS_SETUP:
				result = CheStateEnum.NO_CONTAINERS_SETUP;
				break;

			case CheStateNum.SCAN_GTIN:
				result = CheStateEnum.SCAN_GTIN;
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

		static final byte	INVALID						= -1;
		static final byte	IDLE						= 0;
		static final byte	VERIFYING_BADGE				= 1;
		static final byte	COMPUTE_WORK				= 2;
		static final byte	GET_WORK					= 3;
		static final byte	LOCATION_SELECT				= 4;
		static final byte	CONTAINER_SELECT			= 5;
		static final byte	CONTAINER_POSITION			= 6;
		static final byte	DO_PICK						= 7;
		static final byte	SHORT_PICK					= 8;
		static final byte	PICK_COMPLETE				= 9;
		static final byte	PICK_COMPLETE_CURR_PATH		= 10;	//Added Apr 8, 2015
		static final byte	NO_WORK						= 11;
		// available			= 12;
		static final byte	SHORT_PICK_CONFIRM			= 13;	//Added Oct. 2014
		static final byte	LOCATION_SELECT_REVIEW		= 14;	//Added Dec 12 2014

		//Error States

		//Container Setup Error States
		static final byte	CONTAINER_POSITION_INVALID	= 15;	//Added Dec 29 2014
		static final byte	CONTAINER_SELECTION_INVALID	= 16;	//Addec Dec 30 2014
		static final byte	CONTAINER_POSITION_IN_USE	= 17;	//Added Dec 30 2014
		static final byte	NO_CONTAINERS_SETUP			= 18;	//Added Jan 2 2015

		// new states only used in Line_Scan mode. Many are used in both line_scan and Setup_Orders, so keeping all together.
		static final byte	READY						= 19;
		static final byte	ABANDON_CHECK				= 20;
		// states used in Setup_Orders mode if SCANPICK parameter is set.
		static final byte	SCAN_SOMETHING				= 21;
		static final byte	SCAN_SOMETHING_SHORT		= 22;
		// states used for inventory scan
		static final byte	SCAN_GTIN					= 23;
		// states for put wall
		static final byte	PUT_WALL_SCAN_ORDER			= 24;
		static final byte	PUT_WALL_SCAN_LOCATION		= 25;
		static final byte	PUT_WALL_SCAN_ITEM			= 26;
		static final byte	DO_PUT						= 27;
		static final byte	PUT_WALL_SCAN_WALL			= 29;
		static final byte	GET_PUT_INSTRUCTION			= 30;
		static final byte	NO_PUT_WORK					= 31;
		static final byte	SHORT_PUT					= 32;
		static final byte	SHORT_PUT_CONFIRM			= 33;
		// state for Che Setup Persistence
		static final byte	SETUP_SUMMARY				= 34;	// may result in removal of PICK_COMPLETE_CURR_PATH, PICK_COMPLETE
		// states used for mobile CHE association
		static final byte	REMOTE						= 35;
		static final byte	REMOTE_PENDING				= 36;
		static final byte	REMOTE_LINKED				= 37;

		private CheStateNum() {
		};
	}
}
