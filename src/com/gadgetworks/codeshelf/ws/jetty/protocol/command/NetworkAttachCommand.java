package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkAttachRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkAttachResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class NetworkAttachCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(NetworkAttachCommand.class);

	NetworkAttachRequest request;
	
	@Setter
	private OrganizationDao	organizationDao;
	
	public NetworkAttachCommand(CsSession session, NetworkAttachRequest request) {
		super(session);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		Organization organization = Organization.DAO.findByDomainId(null, request.getOrganizationId());
		if (organization != null) {
			Facility facility = organization.getFacility(request.getFacilityId());
			if (facility != null) {
				CodeshelfNetwork network = facility.getNetwork(request.getNetworkId());
				if (network != null) {
					if (network.isCredentialValid(request.getCredential())) {
						// generate response 
						LOGGER.info("Network "+request.getNetworkId()+"("+organization.getDomainId()+") attached");
						NetworkAttachResponse response = new NetworkAttachResponse();
						response.setStatus(ResponseStatus.Success);
						response.setClassName(network.getClassName());
						response.setNetworkId(network.getPersistentId());
						response.setDomainId(network.getDomainId());
						response.setDescription(network.getDescription());
						return response;
					}
					else {
						LOGGER.warn("Unable to attach network "+request.getNetworkId()+"("+organization.getDomainId()+"): Authentication failed");
						ThreadUtils.sleep(2000);
						NetworkAttachResponse response = new NetworkAttachResponse();
						response.setStatus(ResponseStatus.Authentication_Failed);
						response.setStatusMessage("Invalid credentials");
						return response;
					}
				}
			}
		}
		LOGGER.warn("Unable to attach network "+request.getNetworkId()+"("+request.getOrganizationId()+"): Invalid data");
		NetworkAttachResponse response = new NetworkAttachResponse();
		response.setStatus(ResponseStatus.Fail);
		response.setStatusMessage("Invalid data");
		return response;
	}

}
