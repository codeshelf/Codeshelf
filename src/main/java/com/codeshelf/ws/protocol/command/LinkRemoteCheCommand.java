package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.validation.MethodArgumentException;
import com.codeshelf.ws.protocol.request.LinkRemoteCheRequest;
import com.codeshelf.ws.protocol.response.LinkRemoteCheResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class LinkRemoteCheCommand extends CommandABC {
	private static final Logger			LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private LinkRemoteCheRequest	request;

	private WorkBehavior					workService;

	public LinkRemoteCheCommand(WebSocketConnection connection, LinkRemoteCheRequest request, WorkBehavior workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		LinkRemoteCheResponse response = null;
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			// Get the validated association result
			Che currentAssociatedChe = null;
			try {
				currentAssociatedChe = che.getLinkedToChe();
				// The work service has an explicit clear function. We trigger via null cheName.
				String newAssociateCheName = request.getRemoteCheNameToLinkTo();
				
				if (newAssociateCheName != null && workService.linkCheToCheName(che, newAssociateCheName)) {
					che = Che.staticGetDao().reload(che);
					currentAssociatedChe = che.getLinkedToChe();
				}
				else if (newAssociateCheName == null && workService.clearCheLink(che)) {
					che = Che.staticGetDao().reload(che);
					currentAssociatedChe = che.getLinkedToChe();
				}
				
			} catch (MethodArgumentException e) {
				LOGGER.error("AssociateRemoteCheCommand.exec", e);
				throw e;
			}

			response = new LinkRemoteCheResponse();
			response.setCheName(che.getDomainId());
			if (currentAssociatedChe == null) {
				response.setLinkedCheName(null);
				response.setLinkedCheGuid(null);
			} else {
				response.setLinkedCheName(currentAssociatedChe.getDomainId());
				response.setLinkedCheGuid(currentAssociatedChe.getDeviceGuidStrNoPrefix());				
			}

			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			LOGGER.info("associate response has guid:{} name:{}, assocGuid:{} assocName:{}",response.getNetworkGuid(), response.getCheName(), response.getLinkedCheGuid(), response.getLinkedCheName());
			return response;
		}
		response = new LinkRemoteCheResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}