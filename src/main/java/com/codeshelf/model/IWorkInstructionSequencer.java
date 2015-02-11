/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

import java.util.List;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;

/**
 * Sequences work instructions in an order that they should be executed.  The strategy could be custom per facility, che, path, etc. over time
 * 
 */
public interface IWorkInstructionSequencer {

	// --------------------------------------------------------------------------
	/**
	 * Sort a list of work instructions on a path through a CrossWall
	 * 
	 * @param inFacility
	 * @param inCrosswallWiList
	 * @return
	 */
	public abstract List<WorkInstruction> sort(Facility inFacility, List<WorkInstruction> inCrosswallWiList);

}