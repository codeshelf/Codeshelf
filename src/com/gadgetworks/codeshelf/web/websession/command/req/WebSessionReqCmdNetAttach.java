/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdNetAttach.java,v 1.1 2013/02/17 04:22:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdNetAttach;

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
public class WebSessionReqCmdNetAttach extends WebSessionReqCmdABC {

	private ITypedDao<Organization>	mOrganizationDao;

	public WebSessionReqCmdNetAttach(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Organization> inOrganizationDao) {
		super(inCommandId, inDataNodeAsJson);
		mOrganizationDao = inOrganizationDao;
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.NET_ATTACH_REQ;
	}

	protected final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		String authenticateResult = FAIL;

		JsonNode organizationIdNode = getDataJsonNode().get("organizationId");
		String organizationId = organizationIdNode.getTextValue();
		Organization organization = mOrganizationDao.findByDomainId(null, organizationId);

		if (organization != null) {
			JsonNode facilityIdNode = getDataJsonNode().get("facilityId");
			String facilityId = facilityIdNode.getTextValue();
			Facility facility = organization.getFacility(facilityId);

			if (facility != null) {
				JsonNode codeshelfNetworkIdNode = getDataJsonNode().get("networkId");
				String networkId = codeshelfNetworkIdNode.getTextValue();
				CodeshelfNetwork network = facility.getNetwork(networkId);
				if (network != null) {

					JsonNode credentialNode = getDataJsonNode().get("credential");
					String credential = credentialNode.getTextValue();
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
				result = new WebSessionRespCmdNetAttach(authenticateResult, facility);
			}
		}
		return result;
	}
}
