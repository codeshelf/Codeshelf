/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, Inc., All rights reserved
 *  author jon ranstrom
 *******************************************************************************/
package com.codeshelf.model;

import java.sql.Timestamp;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is a structure for summary values for one set of work instructions
 * We expect the UI to ask for and present this. One of the summaries will lead to a follow on query to get that set of work instructions.
 * A set is defined by work instructions all with the same create time. (Several processes create sets of work instructions, giving all the same time.)
 */

@JsonAutoDetect(getterVisibility = Visibility.PUBLIC_ONLY, fieldVisibility = Visibility.NONE)
public class WiSetSummary implements Comparable<WiSetSummary> {
	@Getter
	private String		cheId, cheDomainId;
	@Getter
	private Timestamp	assignedTime;
	@Getter
	public String		formattedAssignedTime	= "";
	@Getter
	private int			shortCount, invalidCount, newCount, inprogressCount, completeCount, revertCount;

	public WiSetSummary(final Timestamp assignedTime) {
		setAssignedTime(assignedTime);
	}

	public WiSetSummary(final Timestamp assignedTime, String cheId, String cheDomainId) {
		setAssignedTime(assignedTime);
		this.cheId = cheId;
		this.cheDomainId = cheDomainId;
	}

	public int getActiveCount() {
		return invalidCount + newCount + inprogressCount + revertCount;
	}

	@JsonIgnore
	public int getTotal() {
		return invalidCount + newCount + inprogressCount + shortCount + completeCount + revertCount;
	}

	public boolean isActive() {
		return getActiveCount() > 0;
	}

	public void incrementStatus(final WorkInstructionStatusEnum inStatus) {
		switch (inStatus) {
			case INVALID:
				invalidCount++;
				break;
			case NEW:
				newCount++;
				break;
			case INPROGRESS:
				inprogressCount++;
				break;
			case SHORT:
				shortCount++;
				break;
			case COMPLETE:
			case SUBSTITUTION:
				completeCount++;
				break;
			case REVERT:
				revertCount++;
				break;
		}
	}

	public boolean equalCounts(final WiSetSummary inSummary) {
		if (inSummary.getActiveCount() != this.getActiveCount() || inSummary.completeCount != this.completeCount
				|| inSummary.shortCount != this.shortCount) {
			return false;
		}
		return true;
	}

	private void setAssignedTime(Timestamp assignedTime) {
		this.assignedTime = assignedTime;
		computeUnderstandableTimeString();
	}

	public void computeUnderstandableTimeString() {
		if (assignedTime != null) {
			formattedAssignedTime = TimeFormat.getUITime(assignedTime);
		}
	}

	public int compareTo(WiSetSummary compareRun) {
		return assignedTime.compareTo(compareRun.assignedTime);
	}
}
