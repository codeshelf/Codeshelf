/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: LedSample.java,v 1.3 2013/07/19 02:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Data
@Accessors(prefix = "m")
public class PosControllerInstr {

	public static final Byte	POSITION_ALL			= 0;

	// Position controllers can only show 99 items, so we use numbers above 99 for special instructions.
	// We started with 255 and worked down just in case we some day go higher than 99.
	public static final Byte	ZERO_QTY				= (byte) 0;
	public static final Byte	ERROR_CODE_QTY			= (byte) 255;
	public static final Byte	BAY_COMPLETE_QTY		= (byte) 254;
	public static final Byte	POSITION_ASSIGNED_CODE	= (byte) 253;

	public static final Byte	BLINK_FREQ				= (byte) 0x15;
	public static final Byte	BLINK_DUTYCYCLE			= (byte) 0x40;
	public static final Byte	MED_FREQ				= (byte) 0x00;
	public static final Byte	MED_DUTYCYCLE			= (byte) 0xF0;
	public static final Byte	BRIGHT_FREQ				= (byte) 0x00;
	public static final Byte	BRIGHT_DUTYCYCLE		= (byte) 0x40;

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
