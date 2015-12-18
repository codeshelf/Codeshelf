package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.protocol.response.GetOrderDetailWorkResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("wi:get")
public class ComputeDetailWorkCommand extends CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private ComputeDetailWorkRequest request;

	private WorkBehavior	workService;
	
	public ComputeDetailWorkCommand(WebSocketConnection connection, ComputeDetailWorkRequest request, WorkBehavior workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		//TODO: the following line is for testing metrics. Remove ASAP
		ThreadUtils.sleep(1000);
		GetOrderDetailWorkResponse response = null;
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			// Get the work instructions for this CHE and order detail
			response = workService.getWorkInstructionsForOrderDetail(che, request.getOrderDetailId());
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