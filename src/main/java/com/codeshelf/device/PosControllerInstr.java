/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: LedSample.java,v 1.3 2013/07/19 02:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Data
@Accessors(prefix = "m")
public class PosControllerInstr {

	public static final Byte	POSITION_ALL					= 0;

	// Position controllers can only show 99 items, so we use numbers above 99 for special instructions.
	// We started with 255 and worked down just in case we some day go higher than 99.
	public static final Byte	ZERO_QTY						= (byte) 0;

	//Status Codes
	public static final Byte	BAY_COMPLETE_CODE				= (byte) 254;	// sort of fake quantities for now
	public static final Byte	DEFAULT_POSITION_ASSIGNED_CODE	= (byte) 253;
	public static final Byte	REPEAT_CONTAINER_CODE			= (byte) 252;

	//Commenting out bit encodings until v11
	public static final Byte	BITENCODED_SEGMENTS_CODE		= (byte) 240;
	// Bit-encoded LED display characters.
	// https://en.wikipedia.org/wiki/Seven-segment_display
	// MSB->LSB the segments are encoded DP, G, F, E, D, C, B, A
	public static final Byte	BITENCODED_LED_BLANK			= 0x00;
	public static final Byte	BITENCODED_LED_DASH				= 0x40;
	public static final Byte	BITENCODED_LED_O				= 0x5C;
	public static final Byte	BITENCODED_LED_C				= 0x58;
	public static final Byte	BITENCODED_LED_E				= 0x79;

	//Any array mapping digits to their BITENCODED_LED bytes. Digit 9 is at the end of the array.
	public static final byte[]	BITENCODED_DIGITS				= new byte[] { 0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07,
			0x7F, 0x6F											};

	//Display Refresh Freqs
	public static final Byte	BLINK_FREQ						= (byte) 0x15;
	public static final Byte	SOLID_FREQ						= (byte) 0x00;

	//Display Brightness
	public static final Byte	DIM_DUTYCYCLE					= (byte) 0xFD;
	public static final Byte	MED_DUTYCYCLE					= (byte) 0xF0;
	public static final Byte	BRIGHT_DUTYCYCLE				= (byte) 0x40;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "r")
	@Expose
	private byte				mPosition;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "r")
	@Expose
	private byte				mReqQty;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "g")
	@Expose
	private byte				mMinQty;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "b")
	@Expose
	private byte				mMaxQty;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "b")
	@Expose
	private byte				mFreq;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "b")
	@Expose
	private byte				mDutyCycle;

	public PosControllerInstr(final byte inPosition,
		final byte inReqQty,
		final byte inMinQty,
		final byte inMaxQty,
		final byte inFreq,
		final byte inDutyCycle) {
		mPosition = inPosition;
		mReqQty = inReqQty;
		mMinQty = inMinQty;
		mMaxQty = inMaxQty;
		mFreq = inFreq;
		mDutyCycle = inDutyCycle;
	}
}
