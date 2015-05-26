package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.validation.MethodArgumentException;
import com.codeshelf.ws.protocol.request.AssociateRemoteCheRequest;
import com.codeshelf.ws.protocol.response.AssociateRemoteCheResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class AssociateRemoteCheCommand extends CommandABC {
	private static final Logger			LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private AssociateRemoteCheRequest	request;

	private WorkService					workService;

	public AssociateRemoteCheCommand(WebSocketConnection connection, AssociateRemoteCheRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		AssociateRemoteCheResponse response = null;
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			// Get the validated association result
			Che currentAssociatedChe = null;
			try {
				currentAssociatedChe = che.getAssociateToChe();
				// The work service has an explicit clear function. We trigger via null cheName.
				String newAssociateCheName = request.getRemoteCheNameToAssociateTo();
				
				if (newAssociateCheName != null && workService.associateCheToCheName(che, newAssociateCheName)) {
					che = Che.staticGetDao().reload(che);
					currentAssociatedChe = che.getAssociateToChe();
				}
				else if (newAssociateCheName == null && workService.clearCheAssociation(che)) {
					che = Che.staticGetDao().reload(che);
					currentAssociatedChe = che.getAssociateToChe();
				}
				
			} catch (MethodArgumentException e) {
				LOGGER.error("AssociateRemoteCheCommand.exec", e);
				throw e;
			}

			response = new AssociateRemoteCheResponse();
			if (currentAssociatedChe == null) {
				response.setAssociatedCheName(null);
				response.setAssociatedCheGuid(null);
			} else {
				response.setAssociatedCheName(currentAssociatedChe.getDomainId());
				response.setAssociatedCheGuid(currentAssociatedChe.getDeviceGuidStr());				
			}

			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response = new AssociateRemoteCheResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}