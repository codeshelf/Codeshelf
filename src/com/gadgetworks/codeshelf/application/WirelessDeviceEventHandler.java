/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceEventHandler.java,v 1.6 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.IControllerEventListener;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.model.dao.WirelessDeviceDao;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class WirelessDeviceEventHandler implements IControllerEventListener {

	private static final Log	LOGGER	= LogFactory.getLog(WirelessDeviceEventHandler.class);

	private IController			mController;
	private IWirelessDeviceDao	mWirelessDeviceDao;

	//	private List<String>		mDeviceMacAddrsToIgnore;

	// --------------------------------------------------------------------------
	/**
	 *  @param inController
	 *  @param inServerConnectionManager
	 */
	public WirelessDeviceEventHandler(final IController inController, final IWirelessDeviceDao inIWirelessDeviceDao) {

		mController = inController;
		mController.addControllerEventListener(this);
		mWirelessDeviceDao = inIWirelessDeviceDao;
		//		mDeviceMacAddrsToIgnore = new ArrayList<String>();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IControllerEventListener#canNetworkDeviceAssociate(com.gadgetworks.codeshelf.controller.NetMacAddress)
	 */
	public boolean canNetworkDeviceAssociate(final NetMacAddress inMacAddr) {
		boolean result = false;

		WirelessDevice wirelessDevice = mWirelessDeviceDao.findWirelessDeviceByMacAddr(inMacAddr);
		if (wirelessDevice != null) {
			result = true;
		} else {
			//			// If we're not ignoring this device then see if the user wants to add it.
			//			if (!mDeviceMacAddrsToIgnore.contains(inMacAddr)) {
			//				mDeviceMacAddrsToIgnore.add(inMacAddr);
			//				final Runnable ask = new Runnable() {
			//					public void run() {
			//						boolean shouldAssociate = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
			//							LocaleUtils.getStr("should_assoc_dlog.title"),
			//							LocaleUtils.getStr("should_assoc_dlog.prompt", new String[] { inMacAddr }));
			//
			//						if (shouldAssociate) {
			//						} else {
			//
			//						}
			//					}
			//				};
			//				Display.getDefault().asyncExec(ask);
			//			}
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#actorAdded(com.gadgetworks.actor.IActor)
	 */
	public void deviceAdded(INetworkDevice inNetworkDevice) {

		WirelessDevice wirelessDevice = mWirelessDeviceDao.findWirelessDeviceByMacAddr(inNetworkDevice.getMacAddress());

		if (wirelessDevice == null) {
			LOGGER.error("Radio device: " + inNetworkDevice.getMacAddress() + " not found in DB");
		} else {
			///mWirelessDeviceDao.pushNonPersistentUpdates(wirelessDevice);
			try {
				mWirelessDeviceDao.store(wirelessDevice);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#actorLost(com.gadgetworks.actor.IActor)
	 */
	public void deviceLost(INetworkDevice inNetworkDevice) {

		if (inNetworkDevice != null) {
			WirelessDevice wirelessDevice = mWirelessDeviceDao.findWirelessDeviceByMacAddr(inNetworkDevice.getMacAddress());

			if (wirelessDevice != null) {
				wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.LOST);

				//mWirelessDeviceDao.pushNonPersistentUpdates(wirelessDevice);
				try {
					mWirelessDeviceDao.store(wirelessDevice);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#actorRemoved(com.gadgetworks.actor.IActor)
	 */
	public void deviceRemoved(INetworkDevice inNetworkDevice) {

		if (inNetworkDevice != null) {
			WirelessDevice wirelessDevice = mWirelessDeviceDao.findWirelessDeviceByMacAddr(inNetworkDevice.getMacAddress());

			if (wirelessDevice != null) {
				wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.LOST);

				//mWirelessDeviceDao.pushNonPersistentUpdates(wirelessDevice);
				try {
					mWirelessDeviceDao.store(wirelessDevice);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}
	}
}
