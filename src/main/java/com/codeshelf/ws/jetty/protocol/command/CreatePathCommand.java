package com.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Path;
import com.codeshelf.ws.jetty.protocol.request.CreatePathRequest;
import com.codeshelf.ws.jetty.protocol.response.CreatePathResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class CreatePathCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CreatePathCommand.class);

	CreatePathRequest request;
	
	public CreatePathCommand(WebSocketConnection session, CreatePathRequest request) {
		super(session);
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
