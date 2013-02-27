/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: BitFieldInputStream.java,v 1.2 2013/02/27 01:17:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.bitfields;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class BitFieldInputStream {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(BitFieldInputStream.class);

	private InputStream			mBaseInputStream;
	private int					mByteBuffer;
	private int					mCurrentBit;

	// --------------------------------------------------------------------------
	/**
	 * 
	 * @param inInputStream
	 * @param inShouldFrameOutput
	 */
	public BitFieldInputStream(final InputStream inInputStream, final boolean inShouldFrameOutput) {
		mBaseInputStream = inInputStream;
		mByteBuffer = 0;
		mCurrentBit = 0;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Read the next byte from the input stream into the one-byte buffer.  This one-byte
	 *  buffer helps us read partial-byte (n-bit integer) values from the input stream.
	 */
	private void readTheByteBuffer() {
		try {
			mByteBuffer = mBaseInputStream.read();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Read the next 8 bits out of the datastream, MSB-first.
	 *  @return	Return the next byte.
	 */
	private int localRead() throws IOException {

		int result = 0;

		if (mCurrentBit == 0) {
			readTheByteBuffer();
			result = mByteBuffer;
		} else {
			for (int i = 0; i <= 7; i++) {

				if ((mByteBuffer & (0x01 << mCurrentBit)) > 0)
					result |= 0x08 >> i;
				mCurrentBit++;
				if (mCurrentBit > 7) {
					readTheByteBuffer();
					mCurrentBit = 0;
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Read the next inBytesToRead bytes out of the datastream.
	 *  @param inBytes[]	Read the bytes into the array.
	 *  @param inBytesToRead	The number of bytes to read.
	 */
	private void localRead(byte[] inBytes, int inBytesToRead) throws IOException {
		// Optimization for when we're byte-align in the datastream.
		if (mCurrentBit == 0) {
			// Keep trying to read until we get some bytes.
			while (mBaseInputStream.read(inBytes, 0, inBytesToRead) == 0)
				;
		} else {
			for (int i = 0; i < inBytesToRead; i++) {
				inBytes[i] = (byte) localRead();
			}
		}

	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The byte value read.
	 *  @throws IOException
	 */
	public byte readByte() throws IOException {
		return (byte) localRead();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The int (4 byte) value read.
	 *  @throws IOException
	 */
	public short readShort() throws IOException {
		int ch1 = localRead();
		int ch2 = localRead();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (short) ((ch1 << 8) + (ch2 << 0));
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The int (4 byte) value read.
	 *  @throws IOException
	 */
	public int readInt() throws IOException {
		int ch1 = localRead();
		int ch2 = localRead();
		int ch3 = localRead();
		int ch4 = localRead();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The long value read.
	 *  @throws IOException
	 */
	public long readLong() throws IOException {
		long ch1 = ((long) localRead()) << 56;
		long ch2 = ((long) localRead() & 255) << 48;
		long ch3 = ((long) localRead() & 255) << 40;
		long ch4 = ((long) localRead() & 255) << 32;
		long ch5 = ((long) localRead() & 255) << 24;
		long ch6 = ((long) localRead() & 255) << 16;
		long ch7 = ((long) localRead() & 255) << 8;
		long ch8 = ((long) localRead() & 255);
		return ch1 + ch2 + ch3 + ch4 + ch5 + ch6 + ch7 + ch8;
		//		if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0)
		//			throw new EOFException();
		//		return ((long)(ch1 << 56)  + (long)(ch2 << 48) + (long)(ch3 << 40) + (long)(ch4 << 32) + (long)(ch5 << 24) + (long)(ch6 << 16) + (long)(ch7 << 8) + (long)(ch8));
	}

	// --------------------------------------------------------------------------
	/**
	 *  Build up the byte buffer until we have one whole byte and then read it out.
	 *  @param inNBitInt
	 *  @throws IOException
	 */
	public void readNBitInteger(NBitInteger inNBitInt) throws IOException {

		byte newValue = 0;
		// Since we build the buffer from the MSB we need to know the largest (leftmost) bit position with which to start given the largest NBit integer possible.
		//final int maskStarter = 0x01 << (inNBitInt.getBitLen() - 1);

		// Read a byte from the stream.
		if (mCurrentBit == 0)
			localRead();

		// Cycle through the bits in the byte buffer and place them into the new value variable.
		for (int i = 0; i <= inNBitInt.getBitLen() - 1; i++) {

			// If we've gone past the end of the byte buffer then refresh it.
			if (mCurrentBit > 7) {
				readTheByteBuffer();
				mCurrentBit = 0;
			}

			if ((mByteBuffer & (0x80 >> (mCurrentBit))) > 0)
				newValue |= 0x01 << (inNBitInt.getBitLen() - i - 1);
			mCurrentBit++;
		}

		if (mCurrentBit > 7)
			mCurrentBit = 0;

		try {
			inNBitInt.setValue(newValue);
		} catch (OutOfRangeException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inBytes	The byte array to fill with bytes from the input stream.
	 *  @param inBytesToRead	The number of bytes to put into the byte array.
	 *  @throws IOException
	 */
	public void readBytes(byte[] inBytes, int inBytesToRead) throws IOException {
		localRead(inBytes, inBytesToRead);
	}

	// --------------------------------------------------------------------------
	/**
	 *	Read a Pascal-style string from the data stream.
	 *  @return	The string read from the input stream.
	 *  @throws IOException
	 */
	public String readPString() throws IOException {
		byte len = this.readByte();

		if (len > 0) {
			byte[] rawBytes = new byte[len];

			this.readBytes(rawBytes, len);
			return new String(rawBytes);
		} else {
			return "";
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void mark() {
		mBaseInputStream.mark(2);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void reset() {
		try {
			mBaseInputStream.reset();
			mCurrentBit = 0;
			mByteBuffer = 0;
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

}
