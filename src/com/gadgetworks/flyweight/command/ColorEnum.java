/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: ColorEnum.java,v 1.4 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.command;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public enum ColorEnum {

	INVALID(ColorNum.INVALID, "INVALID"),
	RED(ColorNum.RED, "RED"),
	GREEN(ColorNum.GREEN, "GREEN"),
	BLUE(ColorNum.BLUE, "BLUE"),
	MAGENTA(ColorNum.MAGENTA, "MAGENTA"),
	CYAN(ColorNum.CYAN, "CYAN"),
	ORANGE(ColorNum.ORANGE, "ORANGE"),
	BLACK(ColorNum.BLACK, "BLACK"),
	WHITE(ColorNum.WHITE, "WHITE");

	private int		mValue;
	private String	mName;

	// --------------------------------------------------------------------------
	/**
	 *  @param inCmdValue
	 *  @param inName
	 */
	ColorEnum(final int inCmdValue, final String inName) {
		mValue = inCmdValue;
		mName = inName;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inMotorControlCommandID
	 *  @return
	 */
	public static ColorEnum getColorEnum(int inColorNum) {
		ColorEnum result = ColorEnum.INVALID;

		switch (inColorNum) {
			case ColorNum.RED:
				result = ColorEnum.RED;
				break;
			case ColorNum.GREEN:
				result = ColorEnum.GREEN;
				break;
			case ColorNum.BLUE:
				result = ColorEnum.BLUE;
				break;
			case ColorNum.MAGENTA:
				result = ColorEnum.MAGENTA;
				break;
			case ColorNum.CYAN:
				result = ColorEnum.CYAN;
				break;
			case ColorNum.ORANGE:
				result = ColorEnum.ORANGE;
				break;
			case ColorNum.BLACK:
				result = ColorEnum.BLACK;
				break;
			case ColorNum.WHITE:
				result = ColorEnum.WHITE;
				break;
			default:
				break;
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

	final static class ColorNum {
		static final int	INVALID	= -1;
		static final int	BLUE	= 0;
		static final int	RED		= 1;
		static final int	GREEN	= 2;
		static final int	MAGENTA	= 3;
		static final int	CYAN	= 4;
		static final int	ORANGE	= 5;
		static final int	BLACK	= 6;
		static final int	WHITE	= 7;

		private ColorNum() {

		};
	}
}
