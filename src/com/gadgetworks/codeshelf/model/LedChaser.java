/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.flyweight.command.ColorEnum;

/**
 * This is a fire and forget class.
 * You add several normal led lighting jobs. They execute in sequence, the LEDs "chasing" until done.
 * The LED commands fire in the order added.
 * 
 */
public class LedChaser {
	private Facility			mFacility;
	private ColorEnum			mColor;
	private Integer				mSecondsDuration	= 2;
	private ArrayList<LedChase>	mChaseList;
	private Thread				mChaserThread;

	private String				CHASER_THREAD_NAME	= "CHASER";

	public LedChaser(Facility inFacility, ColorEnum inColor) {
		mFacility = inFacility;
		mColor = inColor;
		mChaseList = new ArrayList<LedChase>();
		mChaserThread = null;
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LedChaser.class);

	private class LedChase {
		String	mGuidStr;
		String	mLedCmds;

		private LedChase(String inGuidStr, String inLedCmds) {
			mGuidStr = inGuidStr;
			mLedCmds = inLedCmds;
		}

		private String getGuidStr() {
			return mGuidStr;
		}

		private String getLedCmds() {
			return mLedCmds;
		}
	};

	// Not sure if this will ever be called from outside.
	public void addOneChase(String inGuidStr, final List<LedCmdGroup> inLedCmdGroupList) {
		String theLedCommands = LedCmdGroupSerializer.serializeLedCmdString(inLedCmdGroupList);
		LedChase theChase = new LedChase(inGuidStr, theLedCommands);
		mChaseList.add(theChase);
	}

	private String getGuidStr(LocationABC<?> inLocation) {
		String theGuidStr = "";
		LedController theController = inLocation.getEffectiveLedController();
		if (theController != null) {
			theGuidStr = theController.getDeviceGuidStr();
		}
		return theGuidStr;
	}

	public void addChaseForItem(Item inItem) {
		List<LedCmdGroup> theList = mFacility.getLedCmdGroupListForInventoryItem(inItem, mColor);
		LocationABC<?> location = inItem.getStoredLocation();
		String theGuidStr = getGuidStr(location);
		if (theGuidStr.isEmpty())
			LOGGER.warn("no controller associated for addChaseForItem");
		else
			addOneChase(theGuidStr, theList);
	}

	public void addChaseForLocation(LocationABC<?> inLocation) {
		List<LedCmdGroup> theList = mFacility.getLedCmdGroupListForLocation(inLocation, mColor);
		String theGuidStr = getGuidStr(inLocation);
		if (theGuidStr.isEmpty())
			LOGGER.warn("no controller associated for addChaseForLocation");
		else
			addOneChase(theGuidStr, theList);
	}

	private void sendOneChase(LedChase inLedChase) {
		// String theLedCommands = LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList);
		LOGGER.debug("sendOneChase called");
		LightLedsMessage theMessage = new LightLedsMessage(inLedChase.getGuidStr(), mSecondsDuration, inLedChase.getLedCmds());
		// LightService.sendToAllSiteControllers(mFacility, theMessage);
		// mFacility.sendToAllSiteControllers(mFacility, theMessage);
		// How do oyou call it now?
	}

	public void fireTheChaser(final boolean inTestOnlySoLogOnly) {
		try {
			long millisToSleep = mSecondsDuration * 1000;
			// special cases. 
			if (inTestOnlySoLogOnly)
				millisToSleep = 500;
			// the aisle controller blink rate is 250 on, 500 off. So multiple of 750 is best.
			if (mSecondsDuration == 2)
				millisToSleep = 2250;
			final long passMillsToThread = millisToSleep;

			mChaserThread = new Thread(new Runnable() {

				public void run() {

					if (inTestOnlySoLogOnly)
						LOGGER.info("total chase commands: " + mChaseList.size() + "millis between: " + passMillsToThread);
					for (LedChase aChase : mChaseList) {
						LOGGER.info("send one chase");
						if (!inTestOnlySoLogOnly)
							sendOneChase(aChase);
						try {
							Thread.sleep(passMillsToThread);
						} catch (InterruptedException e) {
						}
					}
				}

			}, CHASER_THREAD_NAME);

			mChaserThread.start();
		} finally {
			// is there something to do to guarantee cleanup of this thread?
		}
	}
}
