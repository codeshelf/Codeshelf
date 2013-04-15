package com.gadgetworks.codeshelf.device;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(prefix = "m")
public class LedValue {

	private static final byte	OFF			= (byte) 0x00;
	private static final byte	ON_			= (byte) 0x2f;

	public static final byte[]	LED_RED		= { ON_, OFF, OFF };
	public static final byte[]	LED_GREEN	= { OFF, ON_, OFF };
	public static final byte[]	LED_BLUE	= { OFF, OFF, ON_ };

	public static final byte[]	LED_ORANGE	= { ON_, ON_, OFF };
	public static final byte[]	LED_CYAN	= { OFF, ON_, ON_ };
	public static final byte[]	LED_MAGENTA	= { ON_, OFF, ON_ };

	public static final byte[]	LED_BLACK	= { OFF, OFF, OFF };
	public static final byte[]	LED_WHITE	= { ON_, ON_, ON_ };

	public Byte					mRed;
	public Byte					mGreen;
	public Byte					mBlue;

	public LedValue(final byte[] inRgbBytes) {
		mRed = inRgbBytes[0];
		mGreen = inRgbBytes[1];
		mBlue = inRgbBytes[2];
	}
}