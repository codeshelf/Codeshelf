/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroupManager.java,v 1.1 2011/01/26 00:30:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.swiftpic;

import java.util.ArrayList;
import java.util.List;

import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;

/**
 * @author jeffw
 *
 */
public final class ControlGroupManager {

	private CodeShelfNetwork			mCodeShelfNetwork;
	private List<ControlGroupInterface>	mControlGroupInterfaces;

	public ControlGroupManager(final CodeShelfNetwork inCodeShelfNetwork) {
		mCodeShelfNetwork = inCodeShelfNetwork;
		mControlGroupInterfaces = new ArrayList<ControlGroupInterface>();
	}

	public void start() {
		// FIrst make sure there's no lingering control groups.
		for (ControlGroupInterface controlGroupInterface : mControlGroupInterfaces) {
			controlGroupInterface.stop();
		}
		mControlGroupInterfaces.clear();
		
		// Now create and start a control group interface for each control group in this network.
		for (ControlGroup controlGroup : mCodeShelfNetwork.getControlGroups()) {
			// Create a control group interface for the control group.
			ControlGroupInterface controlGroupInterface = new ControlGroupInterface(controlGroup);
			controlGroupInterface.start();
		}
	}

	public void stop() {
		for (ControlGroupInterface controlGroupInterface : mControlGroupInterfaces) {
			controlGroupInterface.stop();
		}
	}

}
