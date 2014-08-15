/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAttachWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import com.fasterxml.jackson.databind.JsonNode;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.NetAttachWsRespCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: NET_ATTACH_REQ,
 * 	data {
 * 		organizationId: <organization>
 * 		facilityId: <facility>
 * 		networkId: <deviceId>
 * 		credential: <credential>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class NetAttachWsReqCmd extends WsReqCmdABC {

	private ITypedDao<Organization>	mOrganizationDao;

	public NetAttachWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Organization> inOrganizationDao) {
		super(inCommandId, inDataNodeAsJson);
		mOrganizationDao = inOrganizationDao;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.NET_ATTACH_REQ;
	}

	protected final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		String authenticateResult = FAIL;

		JsonNode organizationIdNode = getDataJsonNode().get("organizationId");
		String organizationId = organizationIdNode.asText();
		Organization organization = mOrganizationDao.findByDomainId(null, organizationId);

		if (organization != null) {
			JsonNode facilityIdNode = getDataJsonNode().get("facilityId");
			String facilityId = facilityIdNode.asText();
			Facility facility = organization.getFacility(facilityId);

			if (facility != null) {
				JsonNode codeshelfNetworkIdNode = getDataJsonNode().get("networkId");
				String networkId = codeshelfNetworkIdNode.asText();
				CodeshelfNetwork network = facility.getNetwork(networkId);
				if (network != null) {

					JsonNode credentialNode = getDataJsonNode().get("credential");
					String credential = credentialNode.asText();
					if (network.isCredentialValid(credential)) {
						authenticateResult = SUCCEED;
					}
				}

				// Sleep for two seconds to slow down brute-force attacks.
				if (!authenticateResult.equals(SUCCEED)) {
					organization = null;
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
					}
				}
				result = new NetAttachWsRespCmd(authenticateResult, network);
			}
		}
		return result;
	}
}
