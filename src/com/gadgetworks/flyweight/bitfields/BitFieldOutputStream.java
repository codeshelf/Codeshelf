/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: BitFieldOutputStream.java,v 1.2 2013/02/27 01:17:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.bitfields;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.controller.IGatewayInterface;

// --------------------------------------------------------------------------
/**
 *  The BitFieldOutputStream is a stream capable of rendering NBitIntegers onto a streaming link such
 *  as a serial i/f connection.  It also employs a SLIP-style framing protocol for robustness across 
 *  that connection.
 *  
 *  @author jeffw
 */
public final class BitFieldOutputStream {

	private static final Logger	LOGGER			= LoggerFactory.getLogger(BitFieldOutputStream.class);

	public static final int		BITS_PER_BYTE	= 8;

	private OutputStream		mBaseOutputStream;
	private byte				mByteBuffer;
	private int					mCurrentBit;

	// --------------------------------------------------------------------------
	/**
	 *  @param inOutputStream
	 */
	public BitFieldOutputStream(final OutputStream inOutputStream) {
		mBaseOutputStream = inOutputStream;
		mByteBuffer = 0;
		mCurrentBit = 0;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Write the one-bte buffer to the output stream.
	 */
	private void outputBytes(byte[] inBytes) {
		try {
			mBaseOutputStream.write(inBytes);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		mByteBuffer = 0;
		mCurrentBit = 0;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Write the one-bte buffer to the output stream.
	 */
	private void outputByteBuffer() {
		try {
			mBaseOutputStream.write(mByteBuffer);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		mByteBuffer = 0;
		mCurrentBit = 0;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inBytes	The bytes to write to the output stream.
	 */
	private void localWrite(byte[] inBytes) {

		localWrite(inBytes, inBytes.length);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inBytes	The bytes to write to the output stream.
	 *  @param inLength The number of bytes from the byte array to write to the output stream.
	 */
	private void localWrite(byte[] inBytes, int inLength) {

		if (mCurrentBit == 0) {
			outputBytes(inBytes);
		} else {

			// For each byte sent to this routine try to fill the byte buffer and then write that byte to the underlying stream.
			for (byte aByte : inBytes) {

				// Try to fill the byte buffer.
				if (mCurrentBit > 0) {
					mByteBuffer = (byte) (mByteBuffer | (aByte >> mCurrentBit));
				} else {
					mByteBuffer = aByte;
				}

				// Write the byte buffer to the stream.
				outputByteBuffer();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	 The idea here is to round out the current byte with zeros.  By writing the currnet byte buffer
	 *   this is the result that we will get.
	 */
	public void roundOutByte() {
		this.outputByteBuffer();
	}

	// --------------------------------------------------------------------------
	/**
	 *  Write all pending data in the one-byte buffer, the call flush() on the output stream.
	 *  @throws IOException
	 */
	public void flush() throws IOException {
		// It would be pretty uncool to write a partial byte to the stream, so let someone know about it.
		if (mCurrentBit != 0) {
			this.outputByteBuffer();
			//throw new IOException("Partial byte written to the end of the stream.");
		}
		mBaseOutputStream.flush();
	}

	// --------------------------------------------------------------------------
	/**
	 *  Write one byte to the output stream.
	 *  @param inByte	The byte to write
	 *  @throws IOException
	 */
	public void writeByte(byte inByte) throws IOException {
		byte[] tempByte = new byte[1];
		tempByte[0] = inByte;
		localWrite(tempByte);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Write a short (2 bytes)  to the output stream.
	 *  @param inShort	The integer to write.
	 *  @throws IOException
	 */
	public void writeShort(int inShort) throws IOException {
		byte[] tempBuffer = new byte[2];
		tempBuffer[0] = (byte) (inShort >>> 8);
		tempBuffer[1] = (byte) (inShort);
		localWrite(tempBuffer);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Write an integer (4 bytes) to the output stream.
	 *  @param inInt	The integer to write.
	 *  @throws IOException
	 */
	public void writeInt(int inInt) throws IOException {
		byte[] tempBuffer = new byte[4];
		tempBuffer[0] = (byte) (inInt >>> 24);
		tempBuffer[1] = (byte) (inInt >>> 16);
		tempBuffer[2] = (byte) (inInt >>> 8);
		tempBuffer[3] = (byte) (inInt);
		localWrite(tempBuffer);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Write a long to the output stream.
	 *  @param inLong	The long to write.
	 *  @throws IOException
	 */
	public void writeLong(long inLong) throws IOException {
		byte[] tempBuffer = new byte[8];
		tempBuffer[0] = (byte) (inLong >>> 56);
		tempBuffer[1] = (byte) (inLong >>> 48);
		tempBuffer[2] = (byte) (inLong >>> 40);
		tempBuffer[3] = (byte) (inLong >>> 32);
		tempBuffer[4] = (byte) (inLong >>> 24);
		tempBuffer[5] = (byte) (inLong >>> 16);
		tempBuffer[6] = (byte) (inLong >>> 8);
		tempBuffer[7] = (byte) (inLong);
		localWrite(tempBuffer);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Build up the byte buffer until we have one whole byte and then write it out.
	 *  @param inNBitInt	An n-bit integer that we want to write to the output stream.
	 *  @throws IOException
	 */
	public void writeNBitInteger(NBitInteger inNBitInt) throws IOException {

		int theBitMask;
		// Since we build the buffer from the MSB we need to know 
		// the largest (leftmost) bit position with which to start 
		// given the largest NBit integer possible.
		final int maskStarter = 0x01 << (inNBitInt.getBitLen() - 1);

		// Loop through all of the bits in the n-bit integer.
		for (int i = 0; i < inNBitInt.getBitLen(); i++) {

			// Put the bits into the byte buffer (MSB first).
			theBitMask = maskStarter >> i;
			if ((inNBitInt.getValue() & theBitMask) > 0)
				// We only ever deal with a byte at a time, so shifting 7 is OK.
				mByteBuffer |= 0x01 << (7 - mCurrentBit);
			mCurrentBit++;

			// If the byte buffer is full the write it to the stream.
			if (mCurrentBit > 7) {
				outputByteBuffer();
				mCurrentBit = 0;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Write a byte array to the output stream.
	 *  @param inBytes	The byte array to write to the output stream.
	 *  @throws IOException
	 */
	public void writeBytes(byte[] inBytes) throws IOException {
		localWrite(inBytes);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Write a byte array to the output stream.
	 *  @param inBytes	The byte array to write to the output stream.
	 *  @throws IOException
	 */
	public void writeBytes(byte[] inBytes, int inLength) throws IOException {
		localWrite(inBytes, inLength);
	}

	// --------------------------------------------------------------------------
	/**
	 *	Write a Pascal-style string to the output stream.
	 *  @param inString	The string to write to the output stream.
	 *  @throws IOException
	 */
	public void writePString(String inString) throws IOException {
		// First write the length byte.
		int len = inString.length();
		if (len > 255)
			len = 255;
		this.writeByte((byte) len);
		this.localWrite(inString.getBytes(), len);
	}

	// --------------------------------------------------------------------------
	/**
	 * Insert a raw END character into the stream (frame end).
	 */
	public void writeEND() {
		try {
			mBaseOutputStream.write(IGatewayInterface.END);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

}
