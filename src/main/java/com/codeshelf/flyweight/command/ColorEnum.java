/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: ColorEnum.java,v 1.4 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.command;

public enum ColorEnum {
	//@EnumValue("INVALID")
	INVALID(ColorNum.INVALID, "INVALID"),
	//@EnumValue("RED")
	RED(ColorNum.RED, "RED"),
	//@EnumValue("GREEN")
	GREEN(ColorNum.GREEN, "GREEN"),
	//@EnumValue("BLUE")
	BLUE(ColorNum.BLUE, "BLUE"),
	//@EnumValue("MAGENTA")
	MAGENTA(ColorNum.MAGENTA, "MAGENTA"),
	//@EnumValue("CYAN")
	CYAN(ColorNum.CYAN, "CYAN"),
	//@EnumValue("ORANGE")
	ORANGE(ColorNum.ORANGE, "ORANGE"),
	//@EnumValue("BLACK")
	BLACK(ColorNum.BLACK, "BLACK"),
	//@EnumValue("WHITE")
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

	public static String getLogCodeOf(ColorEnum inColor) {
		String result = "??";
		switch (inColor) {
			case RED:
				result = "rd";
				break;
			case GREEN:
				result = "gr";
				break;
			case BLUE:
				result = "bl";
				break;
			case MAGENTA:
				result = "mg";
				break;
			case CYAN:
				result = "cy";
				break;
			case ORANGE:
				result = "or";
				break;
			case BLACK:
				result = "bk";
				break;
			case WHITE:
				result = "wt";
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
