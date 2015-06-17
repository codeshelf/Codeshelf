/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: LedSample.java,v 1.3 2013/07/19 02:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.Validatable;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(prefix = "m")
public class PosControllerInstr extends MessageABC implements Validatable {

	public static final Byte	POSITION_ALL					= 0;

	// Position controllers can only show 99 items, so we use numbers above 99 for special instructions.
	// We started with 255 and worked down just in case we some day go higher than 99.
	public static final Byte	ZERO_QTY						= (byte) 0;

	//Status Codes
	public static final Byte	BAY_COMPLETE_CODE				= (byte) 254;											// sort of fake quantities for now
	// public static final Byte	DEFAULT_POSITION_ASSIGNED_CODE	= (byte) 253;
	public static final Byte	REPEAT_CONTAINER_CODE			= (byte) 252;

	//Commenting out bit encodings until v11
	public static final Byte	BITENCODED_SEGMENTS_CODE		= (byte) 240;
	// Bit-encoded LED display characters.
	// https://en.wikipedia.org/wiki/Seven-segment_display
	// MSB->LSB the segments are encoded DP, G, F, E, D, C, B, A
	public static final Byte	BITENCODED_LED_BLANK			= 0x00;
	public static final Byte	BITENCODED_LED_DASH				= 0x40;
	public static final Byte	BITENCODED_LED_O				= 0x5C;												// small o
	public static final Byte	BITENCODED_LED_C				= 0x58;												// small c
	public static final Byte	BITENCODED_LED_E				= 0x79;												// cap E
	public static final Byte	BITENCODED_TRIPLE_DASH			= 0x49;
	public static final Byte	BITENCODED_TOP_BOTTOM			= 0x09;
	// these are the bit encoded versions of the old value kludges. Necessary for 3.1 poscon
	public static final Byte	BITENCODED_LED_B				= 0x7C;												// b
	public static final Byte	BITENCODED_LED_A				= 0x5E;												// a
	public static final Byte	BITENCODED_LED_R				= 0x50;												// r

	//Any array mapping digits to their BITENCODED_LED bytes. Digit 9 is at the end of the array.
	public static final byte[]	BITENCODED_DIGITS				= new byte[] { 0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07,
			0x7F, 0x6F											};

	//Display Refresh Freqs
	public static final Byte	BLINK_FREQ						= (byte) 0x15;
	public static final Byte	SOLID_FREQ						= (byte) 0x00;

	//Display Brightness
	public static final Byte	DIM_DUTYCYCLE					= (byte) 0xFD;
	public static final Byte	MIDDIM_DUTYCYCLE				= (byte) 0xF6;
	public static final Byte	MED_DUTYCYCLE					= (byte) 0xF0;
	public static final Byte	BRIGHT_DUTYCYCLE				= (byte) 0x40;

	private static final Logger	LOGGER							= LoggerFactory.getLogger(PosControllerInstr.class);

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "position")
	@Expose
	private Byte				mPosition;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "reqQty")
	@Expose
	private Byte				mReqQty;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "minQty")
	@Expose
	private Byte				mMinQty;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "maxQty")
	@Expose
	private Byte				mMaxQty;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "freq")
	@Expose
	private Byte				mFreq;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "dutyCycle")
	@Expose
	private Byte				mDutyCycle;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private long				mPostedToPosConController		= 0;

	//----------------------------------------------------
	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "controllerId")
	private String				mControllerId;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "sourceId")
	private String				mSourceId;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "frequency")
	@Expose
	private Frequency			mFrequency						= Frequency.SOLID;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "brightness")
	@Expose
	private Brightness			mBrightness						= Brightness.BRIGHT;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "remove")
	private String				mRemove;

	@Accessors(prefix = "m")
	@Getter
	private boolean				mRemoveAll						= false;

	@Accessors(prefix = "m")
	@Getter
	private List<Byte>			mRemovePos						= new ArrayList<Byte>();

	private String				mRemoveError					= null;

	public PosControllerInstr() {
	}

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

	public PosControllerInstr(final String inControllerId,
		final byte inPosition,
		final byte inReqQty,
		final byte inMinQty,
		final byte inMaxQty,
		final byte inFreq,
		final byte inDutyCycle) {
		this(inPosition, inReqQty, inMinQty, inMaxQty, inFreq, inDutyCycle);
		mControllerId = inControllerId;
	}

	public PosControllerInstr(final String inControllerId,
		final String inSourceId,
		final byte inPosition,
		final byte inReqQty,
		final byte inMinQty,
		final byte inMaxQty,
		final byte inFreq,
		final byte inDutyCycle) {
		this(inPosition, inReqQty, inMinQty, inMaxQty, inFreq, inDutyCycle);
		mControllerId = inControllerId;
		mSourceId = inSourceId;
	}

	/**
	 * This constructor's purpose is to detail a clone
	 */
	public PosControllerInstr(final PosControllerInstr instructionToClone) {
		mPosition = instructionToClone.getPosition();
		mReqQty = instructionToClone.getReqQty();
		mMinQty = instructionToClone.getMinQty();
		mMaxQty = instructionToClone.getMaxQty();
		mFreq = instructionToClone.getFreq();
		mDutyCycle = instructionToClone.getDutyCycle();
		mControllerId = instructionToClone.getControllerId();
		mSourceId = instructionToClone.mSourceId;
	}

	public void processRemoveField() {
		if (mRemove == null) {
			return;
		}
		if ("all".equalsIgnoreCase(mRemove)) {
			mRemoveAll = true;
			return;
		}
		try {
			mRemovePos.clear();
			String[] removePositionsStr = mRemove.split(",");
			for (String positionSrt : removePositionsStr) {
				mRemovePos.add(Byte.parseByte(positionSrt));
			}
		} catch (Exception e) {
			mRemoveError = "Could not process posConCommands.remove";
			LOGGER.error("posConCommands.remove expcts \"all\" or a list of Byte position values");
			LOGGER.error(mRemoveError + " " + e);
		}
	}

	@Override
	public boolean isValid(ErrorResponse errors) {
		boolean valid = true;
		processRemoveField();
		if (mControllerId == null) {
			errors.addErrorMissingBodyParam("posConCommands.controllerId");
			valid = false;
		}
		if (mRemoveError != null) {
			errors.addError(mRemoveError);
			valid = false;
		}
		if (!mRemoveAll && mRemovePos.isEmpty()) {
			if (mPosition == null) {
				errors.addErrorMissingBodyParam("posConCommands.position");
				valid = false;
			}
			if (mReqQty == null) {
				errors.addErrorMissingBodyParam("posConCommands.reqQty");
				valid = false;
			} else if (mReqQty < 0) {
				errors.addError("provide a positive posConCommands.reqQty");
				valid = false;
			}
		}
		return valid;
	}

	public void prepareObject() {
		if (mMinQty == null) {
			mMinQty = mReqQty;
		}
		if (mMaxQty == null) {
			mMaxQty = mReqQty;
		}
		if (mFrequency != null) {
			mFreq = mFrequency.toByte();
		}
		if (mBrightness != null) {
			mDutyCycle = mBrightness.toByte();
		}
	}

	public enum Brightness {
		BRIGHT,
		MEDIUM,
		DIM;

		public Byte toByte() {
			if (this == BRIGHT) {
				return PosControllerInstr.BRIGHT_DUTYCYCLE;
			} else if (this == MEDIUM) {
				return PosControllerInstr.MED_DUTYCYCLE;
			} else if (this == DIM) {
				return PosControllerInstr.DIM_DUTYCYCLE;
			}
			return null;
		}
	}

	public enum Frequency {
		SOLID,
		BLINK;

		public Byte toByte() {
			if (this == SOLID) {
				return PosControllerInstr.SOLID_FREQ;
			} else if (this == BLINK) {
				return PosControllerInstr.BLINK_FREQ;
			}
			return null;
		}
	}

	/**
	 * Very concise, human interpretable description of contents for one poscon. Not json.
	 * This is about what goes out to the site controller, so we would never show the source field, for example. We would add color when that is implemented.
	 * freq and duty should the raw numbers sent. May be slightly hard to interpret. freq 0 = SOLID. duty 64 = BRIGHT, etc.
	 * value = 240 is BITENCODED_SEGMENTS_CODE, which then use the hex codes from min and max for the two LEDs.
	 */
	public String conciseDescription() {
		Byte value = getReqQty();
		String freq = "solid";
		if (getFreq() > 0)
			freq = getFreq().toString();
		Byte position = getPosition();
		String posString = position.toString();
		if (position == 0)
			posString = "all";
		String desc;
		if (value != BITENCODED_SEGMENTS_CODE) {
			desc = String.format("[pos:%s value:%d min:%d, max:%d, freq:%s, duty:%d]",
				posString,
				getReqQty(),
				getMinQty(),
				getMaxQty(),
				freq,
				getDutyCycle());
		} else {
			Byte minValue = getMinQty();
			Byte maxValue = getMaxQty();
			String summary = "??";
			if (minValue == BITENCODED_LED_DASH)
				summary = "'dash'";
			else if (minValue == BITENCODED_LED_C) {  // currently bc or oc are our choices.
				if (maxValue == BITENCODED_LED_O)
					summary = "'oc'";
				else
					summary = "'bc'";
			} else if (minValue == BITENCODED_LED_E)
				summary = "'E'";
			else if (minValue == BITENCODED_LED_A)
				summary = "'a'";
			else if (minValue == BITENCODED_LED_R)
				summary = "'r'";
			else if (minValue == BITENCODED_TRIPLE_DASH)
				summary = "'triple dash'";
			else if (minValue == BITENCODED_TOP_BOTTOM)
				summary = "'double dash'";
			else
			// Remember the "order feedback" possibilities;
			// we might be doing "00" or "08" for last digits of order ID, or "a" if not digits.
			if (maxValue == PosControllerInstr.BITENCODED_DIGITS[0]) {
				summary = "'digits'"; // with the leading zero. Would take a little code to get it back
			} else {
				LOGGER.error("unhandled case in PosControllerInstr.conciseDescription(). BitEncoded flag, with min:{}, max:{}",
					getMinQty(),
					getMaxQty());
			}
			desc = String.format("[pos:%s %s freq:%s, duty:%d]", posString, summary, freq, getDutyCycle());
		}
		return desc;
	}

	/** 
	 * Provide the short display corollary of this message
	 */
	public static PosControllerInstr getCorrespondingShortDisplay(PosControllerInstr instruction) {
		PosControllerInstr revisedInstruction = new PosControllerInstr(instruction);
		revisedInstruction.setMinQty((byte) 0);
		revisedInstruction.setFreq(PosControllerInstr.BLINK_FREQ);

		return revisedInstruction;
	}

	public static class PosConInstrGroupSerializer {
		/**
		 * Duplicated from LedCmdGroupSerializer
		 */

		// Don't expose a constructor.
		private PosConInstrGroupSerializer() {
		}

		// --------------------------------------------------------------------------
		public static String serializePosConInstrString(final List<PosControllerInstr> inPosConInstrGroupList) {
			String result = "";

			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			result = gson.toJson(inPosConInstrGroupList);

			return result;
		}

		// --------------------------------------------------------------------------

		public static List<PosControllerInstr> deserializePosConInstrString(final String inInstrString) {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(inInstrString), "posConInstrString cannot be null");
			List<PosControllerInstr> result = new ArrayList<PosControllerInstr>();

			Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			Type collectionType = new TypeToken<Collection<PosControllerInstr>>() {
			}.getType();
			result = mGson.fromJson(inInstrString, collectionType);

			for (PosControllerInstr instr : result) {
				instr.prepareObject();
			}

			return result;
		}

		// Just a utility checker.
		public static boolean verifyPosConInstrGroupList(final List<PosControllerInstr> inPosConInstrGroupList) {
			boolean allOk = true;
			for (PosControllerInstr posConInstrGroup : inPosConInstrGroupList) {
				ErrorResponse errors = new ErrorResponse();
				if (!posConInstrGroup.isValid(errors)) {
					allOk = false;
				}
				List<String> errorList = errors.getErrors();
				for (String error : errorList) {
					LOGGER.error(error);
				}
			}
			return allOk;
		}
	}

	@Override
	public String getDeviceIdentifier() {
		return this.getControllerId();
	}
}
