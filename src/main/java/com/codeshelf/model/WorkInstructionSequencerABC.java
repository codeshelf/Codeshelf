/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.CompareNullChecker;

/**
 * Sequences work instructions in an order that they should be executed.  The strategy could be custom per facility, che, path, etc. over time
 * 
 */
public abstract class WorkInstructionSequencerABC implements IWorkInstructionSequencer {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(WorkInstructionSequencerABC.class);

	/**
	 * Sort WorkInstructions by their posAlongPath. This use to be identical to CheDeviceLogic.WiDistanceComparator
	 * But now added location and sku tie-breaker sorts
	 */
	class PosAlongPathComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			int value = CompareNullChecker.compareNulls(inWi1, inWi2);
			if (value != 0)
				return value;

			Double wi1Pos = inWi1.getPosAlongPath();
			Double wi2Pos = inWi2.getPosAlongPath();
			value = CompareNullChecker.compareNullsIfBothNullReturnZero(wi1Pos, wi2Pos);
			if (value != 0)
				return value;

			// new behavior in v16. If posAlongPath is unknown (no path, or unmodeled), 
			// then try to look at preferredLocation then sku, both as strings
			if ((wi1Pos == null && wi2Pos == null) || (wi1Pos == 0 && wi2Pos == 0)) {
				// there should be pickInstruction, or the work instruction would not have been made. But some unit tests cheat on this
				String pick1 = inWi1.getPickInstruction();
				String pick2 = inWi2.getPickInstruction();
				if (pick1 == null)
					return 0; // just bail on the foolish case. This is seen.
				value = pick1.compareTo(pick2);
				if (value != 0)
					return value;
				// also, wi should always know its sku
				String sku1 = inWi1.getItemId();
				String sku2 = inWi2.getItemId();
				if (sku1 == null)
					return 0; // just bail on the foolish case. Not seen yet.
				return sku1.compareTo(sku2);
			} else
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
			WorkInstruction.staticGetDao().store(wi);
		}

		return inWiList;
	}

}