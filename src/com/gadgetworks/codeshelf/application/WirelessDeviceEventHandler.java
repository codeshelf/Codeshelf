/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceEventHandler.java,v 1.10 2012/07/19 06:11:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.IControllerEventListener;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class WirelessDeviceEventHandler implements IControllerEventListener {

	private static final Log	LOGGER	= LogFactory.getLog(WirelessDeviceEventHandler.class);

	//	private List<String>		mDeviceMacAddrsToIgnore;

	// --------------------------------------------------------------------------
	/**
	 *  @param inController
	 *  @param inServerConnectionManager
	 */
	public WirelessDeviceEventHandler(final List<IController> inControllerList) {

		for (IController controller : inControllerList) {
			controller.addControllerEventListener(this);			
		}
		//		mDeviceMacAddrsToIgnore = new ArrayList<String>();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IControllerEventListener#canNetworkDeviceAssociate(com.gadgetworks.codeshelf.controller.NetMacAddress)
	 */
	public boolean canNetworkDeviceAssociate(final NetMacAddress inMacAddr) {
		boolean result = false;

		WirelessDevice wirelessDevice = WirelessDevice.DAO.findWirelessDeviceByMacAddr(inMacAddr);
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

		WirelessDevice wirelessDevice = WirelessDevice.DAO.findWirelessDeviceByMacAddr(inNetworkDevice.getMacAddress());

		if (wirelessDevice == null) {
			LOGGER.error("Radio device: " + inNetworkDevice.getMacAddress() + " not found in DB");
		} else {
			///WirelessDevice.DAO.pushNonPersistentUpdates(wirelessDevice);
			try {
				WirelessDevice.DAO.store(wirelessDevice);
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
			WirelessDevice wirelessDevice = WirelessDevice.DAO.findWirelessDeviceByMacAddr(inNetworkDevice.getMacAddress());

			if (wirelessDevice != null) {
				wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.LOST);

				//WirelessDevice.DAO.pushNonPersistentUpdates(wirelessDevice);
				try {
					WirelessDevice.DAO.store(wirelessDevice);
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
			WirelessDevice wirelessDevice = WirelessDevice.DAO.findWirelessDeviceByMacAddr(inNetworkDevice.getMacAddress());

			if (wirelessDevice != null) {
				wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.LOST);

				//WirelessDevice.DAO.pushNonPersistentUpdates(wirelessDevice);
				try {
					WirelessDevice.DAO.store(wirelessDevice);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}
	}
}
