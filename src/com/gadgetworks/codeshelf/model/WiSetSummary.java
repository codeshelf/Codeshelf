/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, Inc., All rights reserved
 *  author jon ranstrom
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This is a structure for summary values for one set of work instructions
 * We expect the UI to ask for and present this. One of the summaries will lead to a follow on query to get that set of work instructions.
 * A set is defined by work instructions all with the same create time. (Several processes create sets of work instructions, giving all the same time.)
 * 
 */

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class WiSetSummary {
	public Timestamp	mWiSetAssignedTime;
	public int			mCompleteCount;
	public int			mShortCount;
	public int			mActiveCount;				// planned and in progress together
	public String		mUnderstandableTimeString;

	// publics? Just want a convenient set to serialize into an array. Change if you want.

	public WiSetSummary(final Timestamp inAssignedTime) {
		mWiSetAssignedTime = inAssignedTime;
		mCompleteCount = 0;
		mShortCount = 0;
		mActiveCount = 0;
		mUnderstandableTimeString = "";
	}
	
	public boolean isActive() {
		return getActiveCount() > 0;
	}

	private void incrementCompletes() {
		mCompleteCount++;
	}
	private void incrementShorts() {
		mShortCount++;
	}
	private void incrementActives() {
		mActiveCount++;
	}
	
	public int getCompleteCount() {
		return mCompleteCount;
	}
	public int getShortCount() {
		return mShortCount;
	}
	public int getActiveCount() {
		return mActiveCount;
	}
	
	public Timestamp getAssignedTime() {
		return mWiSetAssignedTime;
	}
	public String getFormattedAssignedTime() {
		return mUnderstandableTimeString;
	}
	
	public void incrementStatus(final WorkInstructionStatusEnum inStatus) {
		if (inStatus == WorkInstructionStatusEnum.SHORT)
			incrementShorts();
		else if (inStatus == WorkInstructionStatusEnum.COMPLETE)
			incrementCompletes();
		else 
			incrementActives();
	}
	
	public boolean equalCounts(final WiSetSummary inSummary) {
		if (inSummary.mActiveCount != this.mActiveCount)
			return false;
		if (inSummary.mCompleteCount != this.mCompleteCount)
			return false;
		if (inSummary.mShortCount != this.mShortCount)
			return false;
		else
			return true;
	}

	public Timestamp getWiSetCreatedTime() {
		return mWiSetAssignedTime;
	}

	public void computeUnderstandableTimeString() {
		if (mWiSetAssignedTime != null) {
			mUnderstandableTimeString = TimeFormat.getUITime(mWiSetAssignedTime);
		}
	}

}
