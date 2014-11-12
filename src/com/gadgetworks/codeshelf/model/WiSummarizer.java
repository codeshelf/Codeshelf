/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

/**
 * This computes and returns a list of work instruction sets, summarized
 * We expect the UI to ask for and present this. One of the summaries will lead to a follow on query to get that set of work instructions.
 * 
 */
public class WiSummarizer {
	private Map<Timestamp, WiSetSummary>	mWiSetSummaries;

	public WiSummarizer() {
		Comparator<Timestamp> summarizerComparator = Ordering.from(new TimestampComparator()).reverse().nullsLast();
		mWiSetSummaries = new TreeMap<Timestamp, WiSetSummary>(summarizerComparator);
	}

	public List<WiSetSummary> getSummaries() {
		return ImmutableList.<WiSetSummary>copyOf(mWiSetSummaries.values());
	}

	public void computeWiSummariesForChe(UUID inCheId, UUID inFacilityId) {
		if (inCheId == null || inFacilityId == null) {
			return;
		}
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("assignedChe.persistentId", inCheId));
		filterParams.add(Restrictions.eq("parent.persistentId", inFacilityId));
		// wi -> facility
		List<WorkInstruction> wis = WorkInstruction.DAO.findByFilter(filterParams);
		for (WorkInstruction wi : wis) {
			Timestamp wiAssignTime = wi.getAssigned();
			WiSetSummary theSummary = getOrCreateSummaryForTime(wiAssignTime);
			WorkInstructionStatusEnum status = wi.getStatus();
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
			return aSummary.getAssignedTime();
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

	private static class TimestampComparator implements Comparator<Timestamp> {

		@Override
		public int compare(Timestamp o1, Timestamp o2) {
			return o1.compareTo(o2);
		}
		
		
	}
}
