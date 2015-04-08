package com.codeshelf.ws.protocol.command;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Path;
import com.codeshelf.ws.protocol.request.CreatePathRequest;
import com.codeshelf.ws.protocol.response.CreatePathResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("path:create")
public class CreatePathCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CreatePathCommand.class);

	CreatePathRequest request;
	
	public CreatePathCommand(WebSocketConnection connection, CreatePathRequest request) {
		super(connection);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		CreatePathResponse response = new CreatePathResponse();
		try {
			Facility facility = Facility.staticGetDao().findByPersistentId(request.getFacilityId());
			if (facility==null) {
				response.setStatus(ResponseStatus.Fail);
				response.setStatusMessage("Facility not found");
				return response;
			}
			Path path = facility.createPath(request.getDomainId(), request.getPathSegments());
			response.setPath(path);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		catch (Exception e) {
			LOGGER.error("Failed to create path", e);
			response.setStatus(ResponseStatus.Fail);
		}
		return response;
	}
}
