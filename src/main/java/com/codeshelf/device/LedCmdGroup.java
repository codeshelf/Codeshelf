/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: file.java,v 1.1 2010/09/28 05:41:28 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * An LED command group allows: "On device <controller_id>, channel <channel_num> light LEDs 
 * starting at position <pos_num> (going "up" the channel in positions) with the RGB data contained in the array <leds>.
 * 
 * If a work instructions has discontiguous set of LED tubes/channels then we
 * need to send a separate command group for each *contiguopus* set of LEDs to light on a controller's channel.
 * 
 * The LedCmdGroupSerializer will serialize/deserialize an LED command group into a string for DB storage and WebSocket transimission.
 * 
 * 			ctrl:	"<controller_id>",
 * 			chan:	<channel_num>,
 * 			pos:	<pos_num>,
 * 			leds:
 * 				[
 * 					{
 * 						r: <red val>,
 * 						g: <green val>,
 * 						b: <blue val>
 * 					}
 * 				]
 *
 * @author jeffw
 *
 */
public class LedCmdGroup {

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "ctrl")
	@Expose
	private String			mControllerId;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "chan")
	@Expose
	private Short			mChannelNum;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "pos")
	@Expose
	private Short			mPosNum;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@SerializedName(value = "leds")
	@Expose
	private List<LedSample>	mLedSampleList;

	public LedCmdGroup(final String inController, final Short inChannel, final Short inPos, final List<LedSample> inLedSamples) {
		mControllerId = inController;
		mChannelNum = inChannel;
		mPosNum = inPos;
		mLedSampleList = inLedSamples;
	}

	public int compareTo(LedCmdGroup anotherLedCmdGroup) {
		// this could do a compare by controller, then channel, then posNum.
		// but as of V4/v5, only posNum is relevant.
		return Short.compare(getPosNum(), anotherLedCmdGroup.getPosNum());
	}
}
