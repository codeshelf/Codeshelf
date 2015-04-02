package com.codeshelf.ws.protocol.command;

import java.util.List;
import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresRoles;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest.ComputeWorkPurpose;
import com.codeshelf.ws.protocol.response.ComputeWorkResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresRoles("SITECON")
public class GetWorkCommand extends CommandABC {

	ComputeWorkRequest request;
	WorkService workService;
	
	public GetWorkCommand(WebSocketConnection connection, ComputeWorkRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		ComputeWorkResponse response = new ComputeWorkResponse();
		response.setPurpose(ComputeWorkPurpose.GET_WORK);
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			String locationId = request.getLocationId();
			// Figure out the CHE's work area by its scanned location.
			// Facility facility = che.getParent().getParent();
			// Get the work instructions for this CHE at this location for the given containers.
			List<WorkInstruction> wiList = workService.getWorkInstructions(che, locationId, request.getReversePickOrder(), request.getReversePickOrderFromLastTime());
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
