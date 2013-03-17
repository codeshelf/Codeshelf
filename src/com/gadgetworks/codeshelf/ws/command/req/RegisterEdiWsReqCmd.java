/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: RegisterEdiWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.RegisterEdiWsRespCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: REGISTER_EDI_SERVICE,
 * 	data {
 * 		serviceProvider: <service provider code>,
 * 		facilityId: <facility id>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class RegisterEdiWsReqCmd extends WsReqCmdABC {

	private Organization		mOrganization;
	private ITypedDao<Facility>	mFacilityDao;

	public RegisterEdiWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final Organization inOrganization, final ITypedDao<Facility> inFacilityDao) {
		super(inCommandId, inDataNodeAsJson);
		mOrganization = inOrganization;
		mFacilityDao = inFacilityDao;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.REGISTER_EDI_SERVICE_REQ;
	}

	protected final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		String authenticateResult = FAIL;

		// Search for a user with the specified ID (that has no password).

		JsonNode serviceProviderNode = getDataJsonNode().get("serviceProvider");
		String serviceProviderCode = serviceProviderNode.getTextValue();
		Facility facility = mFacilityDao.findByDomainId(mOrganization, serviceProviderCode);

		if (facility != null) {
		}

		result = new RegisterEdiWsRespCmd(authenticateResult, facility);

		return result;
	}
}
