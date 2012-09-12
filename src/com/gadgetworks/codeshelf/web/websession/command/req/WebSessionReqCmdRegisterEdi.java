/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdRegisterEdi.java,v 1.1 2012/09/12 23:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdRegisterEdi;

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
public class WebSessionReqCmdRegisterEdi extends WebSessionReqCmdABC {

	private Organization		mOrganization;
	private ITypedDao<Facility>	mFacilityDao;

	public WebSessionReqCmdRegisterEdi(final String inCommandId, final JsonNode inDataNodeAsJson, final Organization inOrganization, final ITypedDao<Facility> inFacilityDao) {
		super(inCommandId, inDataNodeAsJson);
		mOrganization = inOrganization;
		mFacilityDao = inFacilityDao;
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.REGISTER_EDI_SERVICE;
	}

	protected final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		String authenticateResult = FAIL;

		// Search for a user with the specified ID (that has no password).

		JsonNode serviceProviderNode = getDataJsonNode().get("serviceProvider");
		String serviceProviderCode = serviceProviderNode.getTextValue();
		Facility facility = mFacilityDao.findByDomainId(mOrganization, serviceProviderCode);

		if (facility != null) {
		}

		result = new WebSessionRespCmdRegisterEdi(authenticateResult, facility);

		return result;
	}
}
