/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: LedPos.java,v 1.2 2013/04/02 04:29:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */
@Data
@Accessors(prefix = "m")
public class LedPos {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LedPos.class);

	private Integer				mPosNum = 0;
	private Integer				mCycleNum = 0;
	private List<LedValue>		mValues;

	public LedPos(final Integer inPosNum) {
		mPosNum = inPosNum;
		mValues = new ArrayList<LedValue>();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLedValue
	 */
	public final void addSample(final LedValue inLedValue) {
		mValues.add(inLedValue);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final LedValue getNextSample() {
		LedValue result = mValues.get(mCycleNum++);
		if (mCycleNum >= mValues.size()) {
			mCycleNum = 0;
		}
		return result;
	}
}
