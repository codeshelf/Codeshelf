/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2007, Jeffrey B. Williams, All rights reserved
 *  $Id: NetDataSampleUnitsEnum.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.command;


// --------------------------------------------------------------------------
/**
 * 	These are the data units sent along with scalar value back from the remote sensors.
 * 
 * N.B. Regarding RAW
 * 
 * RAW is used whent he remote device is sending a unitless scalar value directly from the ADAC.
 * This will be the most common case.  Consider the case of a strain gage that we use as a weight
 * and motion scale.  A strain gage is measuring resistance change due to changing forces on a bridging
 * bar.  These forces will change the resistance value of 1 or 2 resistors in a Wheatstone bridge.
 * We then run a reference current through the bridge and the ADAC sample bridge differential voltage
 * in response to the (now) uneven values of the resistors on each leg of the bridge.  Notice that there
 * is no weight involved.  Instead what we do is capture two references measurements - unloaded system,
 * and loaded with a reference mass.  Once we have this the system can compute the weight (or motion)
 * by interpolating a raw ADAC reading on the range between unloaded and loaded.  It is important to
 * note that the idea of weight doesn't exist until the controller system computes it at the server end.
 * 
 * Having said this, we are capable of handling devices that do their own internal calibration and return
 * scalar values in a known unit such as mass, length or temperature.
 * 
 * One other interesting measurement type is "on/off" (Binary).  The value 0 is off, and 1 is on.
 * 
 *  @author jeffw
 */
public enum NetDataSampleUnitsEnum {
	INVALID(NetDataSampleUnitsNum.INVALID, "INVALID"),
	RAW(NetDataSampleUnitsNum.RAW, "R"),
	BINARY(NetDataSampleUnitsNum.BINARY, "B"),
	GRAMS(NetDataSampleUnitsNum.GRAMS, "G"),
	METERS(NetDataSampleUnitsNum.METERS, "M"),
	CENTIGRADE(NetDataSampleUnitsNum.CENTIGRADE, "C"),
	NEWTONS(NetDataSampleUnitsNum.NEWTONS, "N");

	private int		mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	NetDataSampleUnitsEnum(final int inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommandID
	 *  @return
	 */
	public static NetDataSampleUnitsEnum getNetDataSampleUnitsEnum(int inNetDataSampleUnits) {
		NetDataSampleUnitsEnum result = NetDataSampleUnitsEnum.INVALID;

		switch (inNetDataSampleUnits) {
			case NetDataSampleUnitsNum.RAW:
				result = NetDataSampleUnitsEnum.RAW;
				break;
			case NetDataSampleUnitsNum.BINARY:
				result = NetDataSampleUnitsEnum.BINARY;
				break;
			case NetDataSampleUnitsNum.GRAMS:
				result = NetDataSampleUnitsEnum.GRAMS;
				break;
			case NetDataSampleUnitsNum.METERS:
				result = NetDataSampleUnitsEnum.METERS;
				break;
			case NetDataSampleUnitsNum.CENTIGRADE:
				result = NetDataSampleUnitsEnum.METERS;
				break;
			case NetDataSampleUnitsNum.NEWTONS:
				result = NetDataSampleUnitsEnum.METERS;
				break;
			default:
				break;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Given a units value string, return the correct enum.
	 *  @param inDataSampleUnitsID
	 *  @return
	 */
	public static NetDataSampleUnitsEnum getNetDataSampleUnitsEnumFromByte(byte inDataSampleUnitsID) {
		NetDataSampleUnitsEnum result = NetDataSampleUnitsEnum.INVALID;

		if (inDataSampleUnitsID == 'R') {
			result = NetDataSampleUnitsEnum.RAW;
		} else if (inDataSampleUnitsID == 'B') {
			result = NetDataSampleUnitsEnum.BINARY;
		} else if (inDataSampleUnitsID == 'G') {
			result = NetDataSampleUnitsEnum.GRAMS;
		} else if (inDataSampleUnitsID == 'M') {
			result = NetDataSampleUnitsEnum.METERS;
		} else if (inDataSampleUnitsID == 'C') {
			result = NetDataSampleUnitsEnum.CENTIGRADE;
		} else if (inDataSampleUnitsID == 'N') {
			result = NetDataSampleUnitsEnum.NEWTONS;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public int getValue() {
		return mValue;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getName() {
		return mName;
	}

	final static class NetDataSampleUnitsNum {
		static final int	INVALID		= 0;
		static final int	RAW			= 1;
		static final int	BINARY		= 2;
		static final int	GRAMS		= 3;
		static final int	METERS		= 4;
		static final int	CENTIGRADE	= 5;
		static final int	NEWTONS		= 6;

		public NetDataSampleUnitsNum() {

		};
	}

}
