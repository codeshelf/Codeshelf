/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetParameter.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

import java.util.Arrays;


public class NetParameter {

	private int		mParamByteCount;
	private byte[]	mParamValue;

	public NetParameter(final byte[] inParamValue, final int inParamByteCount) {
		if (inParamValue.length != inParamByteCount)
			throw new OutOfRangeException("Net parameter is the wrong size");
		mParamValue = inParamValue;
		mParamByteCount = inParamByteCount;
	}

	public final byte[] getParamValue() {
		return mParamValue;
	}

	public final void setParamValue(final byte[] inParamValue) {
		if (inParamValue.length != mParamByteCount)
			throw new OutOfRangeException("Net parameter is the wrong size");
		mParamValue = inParamValue;
	}
	
	public final String getParamValueBase64() {
		return Arrays.toString(mParamValue);
	}
	
	public final void setParamValueBase64(final String inParamValue) {
		mParamValue = inParamValue.getBytes();
	}
}
