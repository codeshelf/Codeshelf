package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ComputeDetailWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.GetOrderDetailWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class ComputeDetailWorkCommand extends CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private ComputeDetailWorkRequest request;

	private WorkService	workService;
	
	public ComputeDetailWorkCommand(UserSession session, ComputeDetailWorkRequest request, WorkService workService) {
		super(session);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		GetOrderDetailWorkResponse response = new GetOrderDetailWorkResponse();
		String cheId = request.getDeviceId();
		Che che = Che.DAO.findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			// Get the work instructions for this CHE and order detail
			List<WorkInstruction> wiList = workService.getWorkInstructionsForOrderDetail(che, request.getOrderDetailId());
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