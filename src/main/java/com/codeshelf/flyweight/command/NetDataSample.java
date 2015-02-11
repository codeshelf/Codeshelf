/*******************************************************************************
 *  RocketBox
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetDataSample.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.command;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class NetDataSample {

	public static final int			SAMPLE_SIZE_BYTES	= (Integer.SIZE + Integer.SIZE) / 2;

	private int						mNetEndpoint;
	private int						mTimeStamp;
	private int						mScalarValue;
	private NetDataSampleUnitsEnum	mScalarUnits;

	// --------------------------------------------------------------------------
	/**
	 *  @param inTimeStamp
	 *  @param inDataPoint
	 */
	public NetDataSample(final int inNetEndpoint, final int inTimeStamp, final int inScalarValue, final NetDataSampleUnitsEnum inScalarUnits) {
		mNetEndpoint = inNetEndpoint;
		mTimeStamp = inTimeStamp;
		mScalarValue = inScalarValue;
		mScalarUnits = inScalarUnits;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return " time: " + Integer.toHexString(mTimeStamp) + " val: " + Integer.toString(mScalarValue) + " (" + Integer.toHexString(mScalarValue) + ") " + mScalarUnits.toString();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public int getEndpoint() {
		return mNetEndpoint;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public int getTimeStamp() {
		return mTimeStamp;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public int getScalarValue() {
		return mScalarValue;
	}
	
	public NetDataSampleUnitsEnum getScalarUnits() {
		return mScalarUnits;
	}

}
