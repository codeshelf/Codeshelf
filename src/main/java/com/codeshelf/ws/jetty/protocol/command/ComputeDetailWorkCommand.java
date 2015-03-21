package com.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.jetty.protocol.response.GetOrderDetailWorkResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class ComputeDetailWorkCommand extends CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private ComputeDetailWorkRequest request;

	private WorkService	workService;
	
	public ComputeDetailWorkCommand(WebSocketConnection connection, ComputeDetailWorkRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		GetOrderDetailWorkResponse response = null;
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(getTenant(),UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			// Get the work instructions for this CHE and order detail
			response = workService.getWorkInstructionsForOrderDetail(this.getTenant(),che, request.getOrderDetailId());
			// ~bhe: check for null/empty list + handle exception?
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response = new GetOrderDetailWorkResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}