/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2007, Jeffrey B. Williams, All rights reserved
 *  $Id: Util.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.util;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class Util {

	private static final short	ULAW_BIAS		= 0x84;
	private static final short	ULAW_MAX_SAMPLE	= 32767;
	private static final short	ULAW_CLIP		= ULAW_MAX_SAMPLE - ULAW_BIAS;

	// --------------------------------------------------------------------------
	/**
	 * This is not an instance class, so the constructor is private.
	 */
	private Util() {

	}

	public static boolean isOSX() {
		String osName = (System.getProperty("os.name"));
		return osName.startsWith("OS X");
	}

	public static boolean isWindows() {
		String osName = (System.getProperty("os.name"));
		return osName.startsWith("Windows");
	}

	public static boolean isLinux() {
		String osName = (System.getProperty("os.name"));
		return osName.startsWith("Linux");
	}

	public static boolean isSolaris() {
		String osName = (System.getProperty("os.name"));
		return osName.startsWith("Solaris");
	}

	// --------------------------------------------------------------------------
	/**
	 *  Convert a 16bit PCM sample into an 8bit Ulaw sample.
	 *  @param inPCMSample	The PCM sample to convert.
	 *  @return	The Ulaw sample
	 */
	public static byte pcmToUlaw(short inPCMSample) {

		// Convert sample to sign-magnitude
		int sign = inPCMSample & 0x8000;
		if (sign != 0) {
			inPCMSample = (short) -inPCMSample;
			sign = 0x80;
		}

		// Because of the bias that is added, allowing a value larger than CLIP would result in
		// integer overflow, so clip it. 

		if (inPCMSample > ULAW_CLIP)
			inPCMSample = ULAW_CLIP;

		// Convert from 16-bit linear PCM to ulaw
		// Adding this bias guarantees a 1 bit in the exponent region of the data, which is the
		// eight bits to the right of the sign bit.
		inPCMSample += ULAW_BIAS;

		//Exponent value is the position of the first 1 to the right of the sign bit in the
		// exponent region of the data.
		//Find the position of the first 1 to the right of the sign bit, counting from right
		// to left in the exponent region.  The
		// exponent position (value) can range from 0 to 7.
		//Could use a table lookup but will compute on the fly instead because that is better 
		// for teaching the algorithm.
		int exp;
		//Shift sign bit off to the left
		short temp = (short) (inPCMSample << 1);
		for (exp = 7; exp > 0; exp--) {
			if ((temp & 0x8000) != 0)
				break; //found it
			temp = (short) (temp << 1); //shift and loop
		}

		// The mantissa is the four bits to the right of the first 1 bit in the exponent region.
		// Shift those four bits to the four lsb of the 16-bit value.
		temp = (short) (inPCMSample >> (exp + 3));
		//Mask and save those four bits
		int mantis = temp & 0x000f;
		// Construct the complement of the ulaw byte.
		// Set the sign bit in the msb of the 8-bit byte.  
		// The value of sign is either 0x00 or 0x80.
		// Position the exponent in the three bits to the right of the sign bit.
		// Set the 4-bit mantissa in the four lsb of the byte.
		// Note that the one's complement of this value will be returned.
		byte ulawByte = (byte) (sign | (exp << 4) | mantis);
		// Now complement to create actual ulaw byte and return it.
		return (byte) ~ulawByte;
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  Convert an 8bit Ulaw sample into a 16bit PCM sample.
	 *  @param inUlawByte	The Ulaw sample to convert.
	 *  @return	The PCM sample
	 */
	public static short uLawToPCM(byte inUlawByte) {
		// Perform one's complement to undo the one's complement at the end of the encode algorithm.
		inUlawByte = (byte) (~inUlawByte);
		//Get the sign bit from the ulawByte
		int sign = inUlawByte & 0x80;
		//Get the value of the exponent in the three bytes to the right of the sign bit.
		int exp = (inUlawByte & 0x70) >> 4;
		//Get the mantissa by masking off and saving the four lsb in the ulawByte.
		int mantis = inUlawByte & 0xf;
		//Construct the 16-bit output value as type int for simplicity and cast to short  before returning.
		int rawValue = (mantis << (12 - 8 + (exp - 1))) + (132 << exp) - 132;
		//Change the sign if necessary and return the 16-bit estimate of the original sample value.
		return (short) ((sign != 0) ? -rawValue : rawValue);
	}

}
