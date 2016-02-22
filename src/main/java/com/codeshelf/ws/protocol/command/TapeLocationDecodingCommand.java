package com.codeshelf.ws.protocol.command;

import java.util.List;
import java.util.UUID;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.ws.protocol.request.TapeLocationDecodingRequest;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.protocol.response.TapeLocationDecodingResponse;
import com.codeshelf.ws.server.WebSocketConnection;

public class TapeLocationDecodingCommand extends CommandABC<TapeLocationDecodingRequest>{
	
	public TapeLocationDecodingCommand(WebSocketConnection connection, TapeLocationDecodingRequest request) {
		super(connection, request);
	}

	@Override
	public ResponseABC exec() {
		TapeLocationDecodingResponse response = new TapeLocationDecodingResponse();
		String cheId = request.getDeviceId();
		String tapeId = request.getTapeId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			Facility facility = che.getFacility();
			Location location = facility.findSubLocationById(tapeId);
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			response.setNetworkGuid(networkGuid);
			if (location == null) {
				response.setStatusMessage("Can't find Location from tape id " + tapeId);
				response.setStatus(ResponseStatus.Fail);
			} else {
				response.setStatus(ResponseStatus.Success);
				List<LocationAlias> aliases = location.getAliases();
				if (aliases != null && !aliases.isEmpty()) {
					response.setDecodedLocation(aliases.get(0).getAlias());
				} else {
					response.setDecodedLocation(location.getNominalLocationId());
				}
			}
			return response;
		}
		response.setStatusMessage("Can't find CHE with id " + cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
