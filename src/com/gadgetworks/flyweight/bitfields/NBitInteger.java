/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NBitInteger.java,v 1.2 2013/05/26 21:50:40 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.bitfields;

import java.io.IOException;
import java.io.Serializable;

// --------------------------------------------------------------------------
/**
 *  NBitInteger is used for network-related transmission of unsigned integer values encoded into a fixed number of bits.
 *  When reading/writing these values only the specified number of bits are used.
 *  
 *  This is basically an unsigned 8-bit number, but Java doesn't have unsigned types.
 *  For this reason we have to represent it as a short, so that we don't get sign extension for values over 0x7f.
 *  
 *  @author jeffw
 */

public class NBitInteger implements Serializable {

	public static final byte	INIT_VALUE			= 0;
	private static final byte	MAXBITWIDTH			= 8;
	private static final short	MASKING_BITS		= 0x00ff;

	private static final long	serialVersionUID	= -4389961963517642151L;

	private short				mValue;
	private byte				mBitCount;

	// --------------------------------------------------------------------------
	/**
	 *  Creates an n-bit integer.
	 * 
	 *  @param inBitWidth
	 *  @param inNewValue
	 *  @throws IllegalBoundsException - this is a checked exception.  Creating a class that you can't fit is a big problem, 
	 *										and the programmer should discover this.
	 *  @throws OutOfRangeException - this is an unchecked exception.  If this were a checked exception it would propogate tons 
	 *										of try..catch blocks all over the code that really aren't able to deal with this kind 
	 *										of exception.  It seems better to find this in unit testing instead.
	 */
	public NBitInteger(final byte inBitWidth, final byte inNewValue) {

		if ((inBitWidth < 0) || (inBitWidth > MAXBITWIDTH)) {
			throw new IllegalBoundsException("Incorrect bit width.");
		} else {
			mBitCount = inBitWidth;
			setValue(inNewValue);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Make sure that the value we're assigning is within the range that the n-bit integer can hold.
	 *  We now allow -1 to mean "invalid" - this allows us to create uninitialized enums that we can read from the 
	 *  network stream.
	 *  @param inNewValue	The new value we're trying to store.
	 *  @return	True if it is within range.
	 */
	private boolean isInRange(short inNewValue) {
		return (((inNewValue >= 0) && (inNewValue < Math.pow(2, mBitCount))) || (inNewValue == 255));
	}

	/**
	 * Sets a new value for the integer.
	 * 
	 * @param inNewValue The new value.
	 * @throws OutOfRangeException if the value is not between the minimum and maximum value.
	 */
	public final void setValue(byte inNewValue) {
		short convertedValue = (short) (inNewValue & MASKING_BITS);
		if (isInRange(convertedValue)) {
			mValue = convertedValue;
		} else {
			throw new OutOfRangeException("Value is out of range.");
		}
	}

	/**
	 * @return The current value.
	 */
	public final short getValue() {
		return mValue;
	}

	public final byte getBitLen() {
		return mBitCount;
	}

	/**
	 * @return The current minimum.
	 */
	public final short getMin() {
		return 0;
	}

	/**
	 * @return The current maximum.
	 */
	public final short getMax() {
		return (short) (Math.pow(2, mBitCount) - 1);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param out
	 *  @throws IOException
	 */
	private void writeObject(java.io.ObjectOutputStream inOutStream) throws IOException {
		inOutStream.writeInt(mValue);
		inOutStream.writeInt(mBitCount);
	};

	// --------------------------------------------------------------------------
	/**
	 *  @param in
	 *  @throws IOException
	 *  @throws ClassNotFoundException
	 */
	private void readObject(java.io.ObjectInputStream inInStream) throws IOException, ClassNotFoundException {
		mValue = inInStream.readByte();
		mBitCount = inInStream.readByte();
	};

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.valueOf(mValue);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public final boolean equals(Object inObject) {
		if (this == inObject) {
			return true;
		} else if (!(inObject instanceof NBitInteger)) {
			return false;
		} else {
			return (((NBitInteger) inObject).getValue() == this.getValue());
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		return getValue();
	}
}
