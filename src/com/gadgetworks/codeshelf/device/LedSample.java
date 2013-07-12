/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: LedSample.java,v 1.2 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.Arrays;

import com.gadgetworks.flyweight.command.ColorEnum;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(prefix = "m")
public class LedSample {

	public static final byte	OFF			= (byte) 0x00;
	public static final byte	ON_			= (byte) 0xff;

	public static final byte[]	LED_RED		= { ON_, OFF, OFF };
	public static final byte[]	LED_GREEN	= { OFF, ON_, OFF };
	public static final byte[]	LED_BLUE	= { OFF, OFF, ON_ };

	public static final byte[]	LED_ORANGE	= { ON_, ON_, OFF };
	public static final byte[]	LED_CYAN	= { OFF, ON_, ON_ };
	public static final byte[]	LED_MAGENTA	= { ON_, OFF, ON_ };

	public static final byte[]	LED_BLACK	= { OFF, OFF, OFF };
	public static final byte[]	LED_WHITE	= { ON_, ON_, ON_ };

	private Short				mPosition;
	private ColorEnum			mColor;

	public LedSample(final Short inPosition, final ColorEnum inColor) {
		mPosition = inPosition;
		mColor = inColor;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inColor
	 * @return
	 */
	public static byte[] convertColorToBytes(ColorEnum inColor) {
		byte[] result;

		switch (inColor) {
			case RED:
				result = LED_RED;
				break;

			case GREEN:
				result = LED_GREEN;
				break;

			case BLUE:
				result = LED_BLUE;
				break;

			case CYAN:
				result = LED_CYAN;
				break;

			case MAGENTA:
				result = LED_MAGENTA;
				break;

			case ORANGE:
				result = LED_ORANGE;
				break;

			case BLACK:
				result = LED_BLACK;
				break;

			case WHITE:
				result = LED_WHITE;
				break;

			default:
				result = LED_RED;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inBytes
	 * @return
	 */
	public static ColorEnum convertBytesToColor(byte[] inBytes) {
		ColorEnum result = ColorEnum.RED;

		if (Arrays.equals(inBytes, LED_RED)) {
			result = ColorEnum.RED;
		} else if (Arrays.equals(inBytes, LED_GREEN)) {
			result = ColorEnum.GREEN;
		} else if (Arrays.equals(inBytes, LED_BLUE)) {
			result = ColorEnum.BLUE;
		} else if (Arrays.equals(inBytes, LED_CYAN)) {
			result = ColorEnum.CYAN;
		} else if (Arrays.equals(inBytes, LED_MAGENTA)) {
			result = ColorEnum.MAGENTA;
		} else if (Arrays.equals(inBytes, LED_ORANGE)) {
			result = ColorEnum.ORANGE;
		} else if (Arrays.equals(inBytes, LED_BLACK)) {
			result = ColorEnum.BLACK;
		} else if (Arrays.equals(inBytes, LED_WHITE)) {
			result = ColorEnum.WHITE;
		}

		return result;
	}

}
