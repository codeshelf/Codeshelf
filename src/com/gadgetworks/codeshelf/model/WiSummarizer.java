/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

/**
 * This computes and returns a list of work instruction sets, summarized
 * We expect the UI to ask for and present this. One of the summaries will lead to a follow on query to get that set of work instructions.
 * 
 */
public class WiSummarizer {
	private Map<Timestamp, WiSetSummary>	mWiSetSummaries;

	public WiSummarizer() {
		mWiSetSummaries = new HashMap<Timestamp, WiSetSummary>();
	}

	public Collection<WiSetSummary> getSummaries() {
		return mWiSetSummaries.values();
	}

	public void computeWiSummariesForChe(final Che inChe, final Facility inFacility) {
		if (inChe == null || inFacility == null) {
			// LOGGER.error("incorrect use of getAnyValidTimeStampInSummaries");
			return;
		}
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("chePersistentId", inChe.getPersistentId().toString());
		filterParams.put("facilityPersistentId", inFacility.getPersistentId().toString());
		// wi -> orderDetail -> orderHeader -> facility
		for (WorkInstruction wi : WorkInstruction.DAO.findByFilter("assignedChe.persistentId = :chePersistentId and parent.parent.parent.persistentId = :facilityPersistentId",
			filterParams)) {
			Timestamp wiAssignTime = wi.getAssigned();
			WiSetSummary theSummary = getOrCreateSummaryForTime(wiAssignTime);
			WorkInstructionStatusEnum status = wi.getStatusEnum();
			theSummary.incrementStatus(status);
		}
	}

	public int getCountOfSummaries() { // primarily for unit testing
		return mWiSetSummaries.size();
	}

	private WiSetSummary getAnySummary() { // primarily for unit testing
		WiSetSummary foundSummary = null;
		for (WiSetSummary aSummary : mWiSetSummaries.values()) {
			foundSummary = aSummary;
			break;
		}
		return foundSummary;
	}

	public Timestamp getAnySummaryTime() { // primarily for unit testing
		WiSetSummary aSummary = getAnySummary();
		if (aSummary != null)
			return aSummary.getWiSetAssignedTime();
		else
			return null;
	}

	public WiSetSummary getSummaryForTime(final Timestamp inAssignedTime) {
		return mWiSetSummaries.get(inAssignedTime);
	}

	private WiSetSummary getOrCreateSummaryForTime(final Timestamp inAssignedTime) {
		WiSetSummary returnSummary = mWiSetSummaries.get(inAssignedTime);
		if (returnSummary == null) {
			// careful. put returns the old value associated with the key, or null if there was not one.
			// For this purpose, do not return what put gives back.
			returnSummary = new WiSetSummary(inAssignedTime);
			mWiSetSummaries.put(inAssignedTime, returnSummary);
		}
		return returnSummary;
	}

}
