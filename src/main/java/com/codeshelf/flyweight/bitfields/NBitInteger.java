/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NBitInteger.java,v 1.3 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.bitfields;

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

	private static final byte	MAXBITWIDTH			= 16;
	private static final int	MASKING_BITS		= 0x0000ffff;

	private static final long	serialVersionUID	= -4389961963517642151L;

	private int					mValue;
	private byte				mBitCount;

	// --------------------------------------------------------------------------
	/**
	 *  Creates an n-bit integer that has an invalid value.
	 *  This allows us to create an NBitInteger and then later set its value from the bitstream.
	 *  If you try to use it before setting the value you will get an OutOfRangeException();
	 * 
	 *  @param inBitWidth
	 *  @throws IllegalBoundsException - this is a checked exception.  Creating a class that you can't fit is a big problem, 
	 *										and the programmer should discover this.
	 *  @throws OutOfRangeException - this is an unchecked exception.  If this were a checked exception it would propogate tons 
	 *										of try..catch blocks all over the code that really aren't able to deal with this kind 
	 *										of exception.  It seems better to find this in unit testing instead.
	 */
	public NBitInteger(final byte inBitWidth) {
		if ((inBitWidth < 0) || (inBitWidth > MAXBITWIDTH)) {
			throw new IllegalBoundsException("Incorrect bit width.");
		} else {
			mBitCount = inBitWidth;
			mValue = -1;
		}
	}

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
	public NBitInteger(final byte inBitWidth, final short inNewValue) {

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
	 *  @param inNewValue	The new value we're trying to store.
	 *  @return	True if it is within range.
	 */
	private boolean isInRange(int inNewValue) {
		return ((inNewValue >= 0) && (inNewValue < Math.pow(2, mBitCount)));
	}

	/**
	 * Sets a new value for the integer.
	 * 
	 * @param inNewValue The new value.
	 * @throws OutOfRangeException if the value is not between the minimum and maximum value.
	 */
	public final void setValue(byte inNewValue) {
		int convertedValue = (int) (inNewValue & MASKING_BITS);
		if (isInRange(convertedValue)) {
			mValue = convertedValue;
		} else {
			throw new OutOfRangeException("Value is out of range.");
		}
	}
	
	/**
	 * Sets a new value for the integer.
	 * 
	 * @param inNewValue The new value.
	 * @throws OutOfRangeException if the value is not between the minimum and maximum value.
	 */
	public final void setValue(short inNewValue) {
		int convertedValue = (int) (inNewValue & MASKING_BITS);
		if (isInRange(convertedValue)) {
			mValue = convertedValue;
		} else {
			throw new OutOfRangeException("Value is out of range.");
		}
	}

	/**
	 * @return The current value.
	 */
	public final int getValue() {
		if (mValue == -1) {
			throw new OutOfRangeException("Value is out of range.");
		} else {
			return mValue;
		}
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
