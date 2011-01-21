/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceEventHandler.java,v 1.2 2011/01/21 02:22:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.IControllerEventListener;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class WirelessDeviceEventHandler implements IControllerEventListener {

	private static final Log	LOGGER	= LogFactory.getLog(WirelessDeviceEventHandler.class);

	private IController			mController;
//	private List<String>		mDeviceGUIDsToIgnore;

	// --------------------------------------------------------------------------
	/**
	 *  @param inController
	 *  @param inServerConnectionManager
	 */
	public WirelessDeviceEventHandler(final IController inController) {

		mController = inController;
		mController.addControllerEventListener(this);
//		mDeviceGUIDsToIgnore = new ArrayList<String>();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#canActorAssociate(com.gadgetworks.actor.IActor)
	 */
	public boolean canNetworkDeviceAssociate(final String inGUID) {
		boolean result = false;

		WirelessDevice wirelessDevice = Util.getSystemDAO().findWirelessDeviceByGUID(inGUID);
		if (wirelessDevice != null) {
			result = true;
		} else {
			//			// If we're not ignoring this device then see if the user wants to add it.
			//			if (!mDeviceGUIDsToIgnore.contains(inGUID)) {
			//				mDeviceGUIDsToIgnore.add(inGUID);
			//				final Runnable ask = new Runnable() {
			//					public void run() {
			//						boolean shouldAssociate = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
			//							LocaleUtils.getStr("should_assoc_dlog.title"),
			//							LocaleUtils.getStr("should_assoc_dlog.prompt", new String[] { inGUID }));
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

		WirelessDevice wirelessDevice = Util.getSystemDAO().findWirelessDeviceByGUID(inNetworkDevice.getGUID());

		if (wirelessDevice == null) {
			LOGGER.error("Radio device: " + inNetworkDevice.getGUID() + " not found in DB");
		} else {
			///Util.getSystemDAO().pushNonPersistentUpdates(wirelessDevice);
			Util.getSystemDAO().storeWirelessDevice(wirelessDevice);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#actorLost(com.gadgetworks.actor.IActor)
	 */
	public void deviceLost(INetworkDevice inNetworkDevice) {

		if (inNetworkDevice != null) {
			WirelessDevice wirelessDevice = Util.getSystemDAO().findWirelessDeviceByGUID(inNetworkDevice.getGUID());

			if (wirelessDevice != null) {
				wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.LOST);

				//Util.getSystemDAO().pushNonPersistentUpdates(wirelessDevice);
				Util.getSystemDAO().storeWirelessDevice(wirelessDevice);
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.IControllerListener#actorRemoved(com.gadgetworks.actor.IActor)
	 */
	public void deviceRemoved(INetworkDevice inNetworkDevice) {

		if (inNetworkDevice != null) {
			WirelessDevice wirelessDevice = Util.getSystemDAO().findWirelessDeviceByGUID(inNetworkDevice.getGUID());

			if (wirelessDevice != null) {
				wirelessDevice.setNetworkDeviceState(NetworkDeviceStateEnum.LOST);

				//Util.getSystemDAO().pushNonPersistentUpdates(wirelessDevice);
				Util.getSystemDAO().storeWirelessDevice(wirelessDevice);
			}
		}
	}
}
