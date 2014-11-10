/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.CompareNullChecker;

/**
 * Sequences work instructions in an order that they should be executed.  The strategy could be custom per facility, che, path, etc. over time
 * 
 */
public abstract class WorkInstructionSequencerABC implements IWorkInstructionSequencer {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(WorkInstructionSequencerABC.class);

	/**
	 * Sort WorkInstructions by their posAlongPath. This is identical to CheDeviceLogic.WiDistanceComparator
	 */
	private class PosAlongPathComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			int value = CompareNullChecker.compareNulls(inWi1, inWi2);
			if (value != 0)
				return value;

			Double wi1Pos = inWi1.getPosAlongPath();
			Double wi2Pos = inWi2.getPosAlongPath();
			value = CompareNullChecker.compareNulls(wi1Pos, wi2Pos);
			if (value != 0)
				return value;

			return wi1Pos.compareTo(wi2Pos);
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

	// --------------------------------------------------------------------------
	/**
	 * Utility function to set the sort codes. Currently returns the passed in list reference but that could change.
	 * @param inWiList
	 * @return
	 */
	public static List<WorkInstruction> setSortCodesByCurrentSequence(List<WorkInstruction> inWiList) {
		int count = 0;
		for (WorkInstruction wi : inWiList) {
			count++;
			wi.setGroupAndSortCode(String.format("%04d", count));
			WorkInstruction.DAO.store(wi);
		}

		return inWiList;
	}

}