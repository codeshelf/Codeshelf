package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.List;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.GetWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class GetWorkCommand extends CommandABC {

	GetWorkRequest request;
	
	public GetWorkCommand(CsSession session, GetWorkRequest request) {
		super(session);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		GetWorkResponse response = new GetWorkResponse();
		String cheId = request.getCheId();
		Che che = Che.DAO.findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			String locationId = request.getLocationId();
			// Figure out the CHE's work area by its scanned location.
			Facility facility = che.getParent().getParent();
			// Get the work instructions for this CHE at this location for the given containers.
			List<WorkInstruction> wiList = facility.getWorkInstructions(che, locationId);
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
