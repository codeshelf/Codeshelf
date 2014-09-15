/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

/**
 * Sequences work instructions in an order that they should be executed.  The strategy could be custom per facility, che, path, etc. over time
 * 
 */
public abstract class WorkInstructionSequencerABC implements IWorkInstructionSequencer {

	/**
	 * Sort WorkInstructions by their posAlongPath.
	 */
	private class PosAlongPathComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			Double pos1 = inWi1.getPosAlongPath();
			Double pos2 = inWi2.getPosAlongPath();
			// watch out for uninitialized values
			if (pos1 == null && pos2 != null)
				return -1;
			else if (pos2 == null && pos1 != null)
				return 1;
			else if (pos2 == null && pos1 == null)
				return 0;
			else {
				return pos1.compareTo(pos2);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Pre-sort the work instructions by posAlongPath. With that, the location based sorts will have the final sort within location for non-slotted use cases.
	 * @param inWiList
	 * @return
	 */
	protected void preSortByPosAlongPath(List<WorkInstruction> inWiList) {
		Collections.sort(inWiList, new PosAlongPathComparator());
		return;
	}

}