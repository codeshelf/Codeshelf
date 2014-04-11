/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetParameter.java,v 1.4 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.gadgetworks.flyweight.bitfields.OutOfRangeException;

public class NetParameter {

	private int			mParamByteCount;
	private ByteBuffer	mParamBytes;

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
	public final String toString() {
		return getHexStringWithPrefix();
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Return the parameter as a hex string with a "0x" prefix.
	 * @return
	 */
	public final String getHexStringWithPrefix() {
		return "0x" + getByteArrayAsString(mParamBytes.array());
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the parameter as a hex string with no "0x" prefix.
	 * @return
	 */
	public final String getHexStringNoPrefix() {
		return getByteArrayAsString(mParamBytes.array());
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
		mParamBytes = ByteBuffer.wrap(inByteArray);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final byte[] getParamValueAsByteArray() {
		return mParamBytes.array();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inByteArray
	 * @return
	 */
	private String getByteArrayAsString(byte[] inByteArray) {
		StringBuffer result = new StringBuffer("");

		byte[] bytes = mParamBytes.array();
		for (int i = 0; i < bytes.length; i++) {
			String hexByte = Integer.toHexString(bytes[i]);
			if (hexByte.length() > 2) {
				hexByte = hexByte.substring(hexByte.length() - 2);
			} else if (hexByte.length() == 1) {
				result.append("0");
			}
			result.append(hexByte);

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
			if (output.length() < 2) {
				output += "0";
			}
			//convert hex to byte
			result[i / 2] = (byte) (0xff & Short.parseShort(output, 16));
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object inObject) {
		boolean result = false;
		if (inObject instanceof NetParameter) {
			NetParameter netParam = (NetParameter) inObject;
			result = Arrays.equals(mParamBytes.array(), netParam.getParamValueAsByteArray());
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		return mParamBytes.hashCode();
	}
}
