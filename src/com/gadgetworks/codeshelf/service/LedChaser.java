/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.service;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.util.concurrent.ForwardingFuture;

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
	private LinkedList<ILocation<?>>	mChaseList;
	private ScheduledExecutorService mExecutorService ;
	private Future<?>    		mLastChaserFuture;
	private LightService 		mLightService;
	
	public LedChaser(Facility inFacility, ColorEnum inColor, LightService lightService) {
		mFacility = inFacility;
		mColor = inColor;
		mChaseList = new LinkedList<ILocation<?>>();
		mExecutorService = Executors.newSingleThreadScheduledExecutor();
		mLightService = lightService;
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

	private String getGuidStr(LocationABC inLocation) {
		String theGuidStr = "";
		LedController theController = inLocation.getEffectiveLedController();
		if (theController != null) {
			theGuidStr = theController.getDeviceGuidStr();
		}
		return theGuidStr;
	}

	public void addChaseForLocation(LocationABC inLocation) {
		mChaseList.add(inLocation);
	}

	public Future<?> fireTheChaser(final boolean inTestOnlySoLogOnly) {
		if (mLastChaserFuture != null) {
			mLastChaserFuture.cancel(true);
		}
		
		long millisToSleep = mSecondsDuration * 1000;
		final LinkedList<ILocation<?>> chaseListToFire = (LinkedList<ILocation<?>>) mChaseList.clone();
		
		//Future that ends on exception when pop returns nothing
		Future<?> scheduledFuture = mExecutorService.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				ILocation<?> ledChase = chaseListToFire.pop();
				mLightService.lightOneLocation(mColor.toString(), mFacility.getPersistentId().toString(), ledChase.getNominalLocationId());			}
		}, 0, millisToSleep, TimeUnit.MILLISECONDS);
		
		//Wrap in a future that hides the NoSuchElementException semantics
		mLastChaserFuture = new ForwardingFuture.SimpleForwardingFuture(scheduledFuture) {
			
			public Object get() throws ExecutionException, InterruptedException {
				try {
					return delegate().get();
				} catch (ExecutionException e) {
					if (!(e.getCause() instanceof NoSuchElementException)) {
						throw e;
					}
				}
				return null;
			}
		};
		return mLastChaserFuture;
	}
}
