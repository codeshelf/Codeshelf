/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetParameter.java,v 1.4 2011/01/22 07:58:31 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

import org.apache.commons.codec.binary.Hex;

public class NetParameter {

	private int		mParamByteCount;
	private byte[]	mParamBytes;

	public NetParameter(final String inHexParamValueString, final int inParamByteCount) {
		mParamByteCount = inParamByteCount;
		setParamValueFromString(inHexParamValueString);
	}

	public NetParameter(final byte[] inHexParamValueByte, final int inParamByteCount) {
		mParamByteCount = inParamByteCount;
		setParamValueFromByteArray(inHexParamValueByte);
	}

	public NetParameter(final int inParamByteCount) {
		mParamByteCount = inParamByteCount;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getByteArrayAsString(mParamBytes);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inParamValueString
	 */
	public final void setParamValueFromString(final String inParamValueString) {
		if (!inParamValueString.startsWith("0x"))
			throw new OutOfRangeException("Net parameter must start with 0x");
		byte[] tempArray = convertStringToByteArray(inParamValueString.substring("0x".length()));
		setParamValueFromByteArray(tempArray);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inByteArray
	 */
	public final void setParamValueFromByteArray(byte[] inByteArray) {
		if (inByteArray.length != mParamByteCount)
			throw new OutOfRangeException("Net parameter is the wrong size");
		mParamBytes = inByteArray;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final byte[] getParamValueAsByteArray() {
		return mParamBytes;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inByteArray
	 * @return
	 */
	private String getByteArrayAsString(byte[] inByteArray) {
		StringBuffer result = new StringBuffer("0x");
		
		for (int i = 0; i < mParamBytes.length; i++) {
			result.append(Integer.toHexString(mParamBytes[i]));
		}

		return result.toString();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inString
	 * @return
	 */
	private byte[] convertStringToByteArray(String inString) {
		byte[] result = new byte[mParamByteCount];

		for (int i = 0; i < inString.length() - 1; i += 2) {
			//grab the hex in pairs
			String output = inString.substring(i, (i + 2));
			//convert hex to byte
			result[i / 2] = Byte.parseByte(output, 16);
		}

		return result;
	}
}
