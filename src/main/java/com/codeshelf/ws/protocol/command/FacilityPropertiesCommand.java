package com.codeshelf.ws.protocol.command;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FacilityProperty;
import com.codeshelf.ws.protocol.request.FacilityPropertiesRequest;
import com.codeshelf.ws.protocol.response.FacilityPropertiesResponse;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("ux:get")
public class FacilityPropertiesCommand extends CommandABC<FacilityPropertiesRequest> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(FacilityPropertiesCommand.class);
		
	public FacilityPropertiesCommand(WebSocketConnection connection, FacilityPropertiesRequest request) {
		super(connection, request);
	}
	
	@Override
	public FacilityPropertiesResponse exec() {
		FacilityPropertiesResponse response = new FacilityPropertiesResponse();
		String persistentId = request.getPersistentId();
		Facility facility = Facility.staticGetDao().findByPersistentId(persistentId);
		if (facility != null) {
			List<FacilityProperty> properties = PropertyBehavior.getAllProperties(facility);
			response.setResults(properties);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Unable to find Facility {} to retrieve its properties", persistentId);
		response.setStatus(ResponseStatus.Fail);
		response.setStatusMessage("Unable to find Facility " + persistentId + " to retrieve its properties");
		return response;
	}
	
}
