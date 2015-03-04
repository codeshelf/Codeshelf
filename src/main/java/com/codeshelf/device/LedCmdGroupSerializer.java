/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CsDeviceManager.java,v 1.6 2013/04/15 21:27:05 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Because the remote gateway controller has no business logic or persistence it cannot compute how 
 * to light LEDs for a set of work instructions.  Because the layout of a facility w.r.t. LED controllers
 * might be quite complex, we need a simple, sure-fire way to tell the gateway controller how to light
 * LEDs for a work instruction.  This class processes this command stream into a string that we can
 * serialize to the remote gateway controller that it can deserialize into a set of commands
 * it sends on the facility's radio network.
 * 
 * The encoding is JSON.
 * 
 * The format of the stream is:
 * 
 * [
 * 		{
 * 			ctrl:	"controller mac addr",
 * 			chan:	"channel number",
 * 			pos:	"position number",
 * 			leds:
 * 				[
 * 					{
 * 						r: "red val",
 * 						g: "green val",
 * 						b: "blue val"
 * 					}
 * 				]
 * 		}
 * ]
 * 
 * @author jeffw
 *
 */
public final class LedCmdGroupSerializer {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LedCmdGroupSerializer.class);
	
	// Don't expose a constructor.
	private LedCmdGroupSerializer() {
		
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public static String serializeLedCmdString(final List<LedCmdGroup> inLedCmdGroupList) {
		String result = "";

		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		result = gson.toJson(inLedCmdGroupList);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCmdString
	 * @return
	 */
	public static List<LedCmdGroup> deserializeLedCmdString(final String inCmdString) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(inCmdString), "ledCmdString cannot be null");
		List<LedCmdGroup> result = new ArrayList<LedCmdGroup>();

		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Type collectionType = new TypeToken<Collection<LedCmdGroup>>() {
		}.getType();
		result = mGson.fromJson(inCmdString, collectionType);

		return result;
	}
	
	// Just a utility checker.
	public static boolean verifyLedCmdGroupList(final List<LedCmdGroup> inLedCmdGroupList){
		boolean allOk = true;
		for (LedCmdGroup ledCmdGroup : inLedCmdGroupList) {
			Short dsChannnel = ledCmdGroup.getChannelNum();
			if (dsChannnel == null){
				LOGGER.error("bad channeld in verifyCommandString");
				allOk = false;
			}
			for (LedSample dsLedSample : ledCmdGroup.getLedSampleList()) {
				if (dsLedSample.getPosition() == null){
					// only do this warn once
					if (allOk) 
						LOGGER.error("Poorly formed LED commands");
					allOk = false;
				}
			}
		}
		return allOk;

	}

}
