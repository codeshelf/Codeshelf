/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

/**
 * This computes and returns a list of work instruction sets, summarized
 * We expect the UI to ask for and present this. One of the summaries will lead to a follow on query to get that set of work instructions.
 * 
 */
public class WiFactory {

	public WiFactory() {

	}

	public static WorkInstruction createHouseKeepingWi(WorkInstructionTypeEnum inType, Facility inFacility, WorkInstruction inPrevWi, WorkInstruction inNextWi) {

		return null;
	}
}
