package com.codeshelf.ws.protocol.message;

import java.util.HashMap;
import java.util.List;


import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ContainerUse;

/**
 * 	This is only about the cart setup (orders/containers vs. position). It is not a general purpose CHE updater.
 * 	This communicates via the CHE's GUID as most similar messages do, which gives simplest implementation on site controller.
 * 	FYI, "networkUpdate" currently blasts out all CHEs, supporting these current business cases:
 * 	- Change the CHE GUID
 * 	- Change the CHE color
 * 	- Change the CHE process mode
 * 
 */
public class CheStatusMessage extends MessageABC {

	@Getter
	@Setter
	String						cheGuid;

	@Getter
	HashMap<String, Integer>	containerPositions;

	public CheStatusMessage() {
	}

	public CheStatusMessage(Che che) {
		setCheGuid(che.getDeviceNetGuid().getHexStringNoPrefix());

		List<ContainerUse> uses = che.getUses();
		if (uses != null) {
			containerPositions = new HashMap<String, Integer>();
			for (ContainerUse use : uses) {
				this.containerPositions.put(use.getContainerName(), use.getPosconIndex());
			}
		}
	}
}
