/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroupManager.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.ArrayList;
import java.util.List;

import com.gadgetworks.codeshelf.model.TagProtocolEnum;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;

/**
 * @author jeffw
 *
 */
public final class ControlGroupManager {

	private CodeShelfNetwork			mCodeShelfNetwork;
	private List<IControllerConnection>	mControlGroupConnections;

	public ControlGroupManager(final CodeShelfNetwork inCodeShelfNetwork) {
		mCodeShelfNetwork = inCodeShelfNetwork;
		mControlGroupConnections = new ArrayList<IControllerConnection>();
	}

	public void start() {
		// FIrst make sure there's no lingering control groups.
		for (IControllerConnection controlGroupInterface : mControlGroupConnections) {
			controlGroupInterface.stop();
		}
		mControlGroupConnections.clear();

		// Now create and start a control group interface for each control group in this network.
		for (ControlGroup controlGroup : mCodeShelfNetwork.getControlGroups()) {
			// Create a control group interface for the control group.
			if (controlGroup.getTagProtocol().equals(TagProtocolEnum.ATOP)) {
				IControllerConnection controlGroupInterface = new AtopControllerConnection(controlGroup);
				controlGroupInterface.start();
			}
		}
	}

	public void stop() {
		for (IControllerConnection controlGroupInterface : mControlGroupConnections) {
			controlGroupInterface.stop();
		}
	}

}
