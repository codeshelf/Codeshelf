/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CheWorkWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.ws.command.resp.CheGetWorkRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: CHE_GETWORK_REQ,
 * 	data {
 * 		persistentId: 	<persistentId>,
 * 		locationId: 	<locationId>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class CheGetWorkReqCmd extends WsReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheGetWorkReqCmd.class);

	private ITypedDao<Che>		mCheDao;

	public CheGetWorkReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Che> inCheDao) {
		super(inCommandId, inDataNodeAsJson);
		mCheDao = inCheDao;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.CHE_GETWORK_REQ;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdABC#doExec()
	 */
	protected final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		JsonNode persistentIdNode = getDataJsonNode().get("persistentId");
		String persistentId = persistentIdNode.asText();
		Che che = mCheDao.findByPersistentId(UUID.fromString(persistentId));

		if (che != null) {
			JsonNode locationIdNode = getDataJsonNode().get("locationId");
			String locationId = locationIdNode.asText();

			result = new CheGetWorkRespCmd(che, locationId);
		}
		return result;
	}
}
