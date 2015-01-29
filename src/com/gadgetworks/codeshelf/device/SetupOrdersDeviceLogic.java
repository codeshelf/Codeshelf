/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

	import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;

	/**
	 * @author jonranstrom
	 *
	 */
	public class SetupOrdersDeviceLogic extends CheDeviceLogic {
		// This code runs on the site controller, not the CHE.
		// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

		private static final Logger	LOGGER									= LoggerFactory.getLogger(SetupOrdersDeviceLogic.class);


		public SetupOrdersDeviceLogic(final UUID inPersistentId,
			final NetGuid inGuid,
			final ICsDeviceManager inDeviceManager,
			final IRadioController inRadioController) {
			super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		}



}
