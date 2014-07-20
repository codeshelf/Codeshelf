package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ComputeWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ComputeWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class ComputeWorkCommand extends CommandABC {

	ComputeWorkRequest request;
	
	public ComputeWorkCommand(ComputeWorkRequest request) {
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		ComputeWorkResponse response = new ComputeWorkResponse();
		String cheId = request.getCheId();
		Che che = Che.DAO.findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			// Figure out the CHE's work area by its scanned location.
			Facility facility = che.getParent().getParent();
			// Get the work instructions for this CHE at this location for the given containers.
			Integer wiCount = facility.computeWorkInstructions(che, request.getContainerIds());
			// ~bhe: should we check for null/zero and return a different status?
			response.setWorkInstructionCount(wiCount);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response.setStatusMessage("Can't find CHE with id "+cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
