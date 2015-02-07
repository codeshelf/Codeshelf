package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.List;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.GetWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class GetWorkCommand extends CommandABC {

	GetWorkRequest request;
	WorkService workService;
	
	public GetWorkCommand(UserSession session, GetWorkRequest request, WorkService workService) {
		super(session);
		this.request = request;
		this.workService = workService;
	}

	@SuppressWarnings("unused")
	@Override
	public ResponseABC exec() {
		GetWorkResponse response = new GetWorkResponse();
		String cheId = request.getDeviceId();
		Che che = Che.DAO.findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			String locationId = request.getLocationId();
			// Figure out the CHE's work area by its scanned location.
			// Facility facility = che.getParent().getParent();
			// Get the work instructions for this CHE at this location for the given containers.
			List<WorkInstruction> wiList = workService.getWorkInstructions(che, locationId);
			// ~bhe: check for null/empty list + handle exception?
			response.setWorkInstructions(wiList);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
