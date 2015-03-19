package com.codeshelf.ws.jetty.protocol.command;

import java.util.List;
import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresRoles;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.codeshelf.ws.jetty.protocol.response.GetWorkResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

//@RequiresRoles("SITECON")
public class GetWorkCommand extends CommandABC {

	GetWorkRequest request;
	WorkService workService;
	
	public GetWorkCommand(WebSocketConnection connection, GetWorkRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		GetWorkResponse response = new GetWorkResponse();
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			String locationId = request.getLocationId();
			// Figure out the CHE's work area by its scanned location.
			// Facility facility = che.getParent().getParent();
			// Get the work instructions for this CHE at this location for the given containers.
			List<WorkInstruction> wiList = workService.getWorkInstructions(che, locationId, request.getReversePickOrder(), request.getReverseOrderFromLastTime());
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
