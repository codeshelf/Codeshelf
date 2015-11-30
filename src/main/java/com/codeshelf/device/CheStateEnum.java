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
	INVALID, IDLE, VERIFYING_BADGE, COMPUTE_WORK, GET_WORK, CONTAINER_SELECT, CONTAINER_POSITION, DO_PICK, SHORT_PICK, SHORT_PICK_CONFIRM,
	CONTAINER_POSITION_INVALID, CONTAINER_SELECTION_INVALID, CONTAINER_POSITION_IN_USE, NO_CONTAINERS_SETUP, LOW_CONFIRM,

	// new states only used in Line_Scan mode. Many are used in both line_scan and Setup_Orders, so keeping all together.
	READY, ABANDON_CHECK,

	// states used in Setup_Orders mode if SCANPICK parameter is set.
	SCAN_SOMETHING, SCAN_SOMETHING_SHORT,

	// states used for inventory scan DEV-644
	SCAN_GTIN,

	// states used for put wall DEV-708
	PUT_WALL_SCAN_LOCATION, PUT_WALL_SCAN_ORDER,

	// states used for put wall DEV-712, DEV-713
	PUT_WALL_SCAN_WALL, PUT_WALL_SCAN_ITEM, PUT_WALL_POSCON_BUSY, DO_PUT, GET_PUT_INSTRUCTION, NO_PUT_WORK, SHORT_PUT, SHORT_PUT_CONFIRM,

	//states used for sku/return wall DEV-956
	SKU_WALL_SCAN_GTIN_LOCATION, SKU_WALL_ALTERNATE_WALL_AVAILABLE,
	
	// state for Che setup persistence
	SETUP_SUMMARY,

	// states used for mobile CHE association
	REMOTE, REMOTE_PENDING, REMOTE_LINKED,
	
	// states for the INFO command
	INFO_PROMPT, INFO_RETRIEVAL, INFO_DISPLAY,
	
	// states for the REMOVE command
	REMOVE_CONFIRMATION, REMOVE_CHE_CONTAINER,
	
	// states for PALLETIZER
	PALLETIZER_SCAN_ITEM, PALLETIZER_PROCESSING, PALLETIZER_PUT_ITEM, PALLETIZER_NEW_ORDER, PALLETIZER_DAMAGED, PALLETIZER_REMOVE;
}
