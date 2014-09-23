/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

/**
 * Sequences work instructions in an order that they should be executed.  The strategy could be custom per facility, che, path, etc. over time
 * 
 */
public abstract class WorkInstructionSequencerABC implements IWorkInstructionSequencer {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkInstructionSequencerABC.class);

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

	// --------------------------------------------------------------------------
	/**
	 * Are the workInstructions coming/going to the same cart position? If so, the user may be confused about the button press. We want an intervening housekeeping WI
	 * @param prevWi
	 * @param nextWi
	 * @return
	 */
	private boolean wisNeedHouseKeepingBetween(WorkInstruction inPrevWi, WorkInstruction inNextWi) {
		if (inPrevWi == null)
			return false;
		else if (inNextWi == null) {
			LOGGER.error("null value in wisNeedHouseKeepingBetween");
			return false;
		}
		else { // expand this
			return false;
		}
	}
	
	private WorkInstruction getNewHousekeepingWiSimilarTo(Facility inFacility, WorkInstruction inWi) {
		// expand this. Call through to facility
		return null;
	}	

	// --------------------------------------------------------------------------
	/**
	 * This will add any necessary housekeeping WIs. And then add the sort and group codes and save each WI.
	 * @param inSortedWiList
	 * @return
	 */
	public List<WorkInstruction> addHouseKeepingAndSaveSort(Facility inFacility, List<WorkInstruction> inSortedWiList) {
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		WorkInstruction lastWi = null;
		for (WorkInstruction wi : inSortedWiList) {
			if (wisNeedHouseKeepingBetween(lastWi, wi)) {
				WorkInstruction houseKeepingWi = getNewHousekeepingWiSimilarTo(inFacility, wi);
				if (houseKeepingWi != null)
					wiResultList.add(houseKeepingWi);
				else 
					LOGGER.error("null returned from getNewHousekeepingWiSimilarTo");
			}
			wiResultList.add(wi);
		}
		// At this point, some or none added. wiResultList is ordered. Time to add the sort codes.
		int count = 0;
		for (WorkInstruction wi : wiResultList) {
			count++;
			wi.setGroupAndSortCode(String.format("%04d", count));
			WorkInstruction.DAO.store(wi);
		}

		return wiResultList;
	}

}