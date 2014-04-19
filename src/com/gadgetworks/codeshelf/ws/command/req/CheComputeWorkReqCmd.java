/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CheWorkWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.ws.command.resp.CheComputeWorkRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: CHE_COMPUTEWORK_REQ,
 * 	data {
 * 		persistentId: 	<persistentId>,
 * 		containerIds: [
 * 			{
 * 				containerId: <containerId>
 * 			}
 * 		]
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class CheComputeWorkReqCmd extends WsReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheComputeWorkReqCmd.class);

	private ITypedDao<Che>		mCheDao;

	public CheComputeWorkReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Che> inCheDao) {
		super(inCommandId, inDataNodeAsJson);
		mCheDao = inCheDao;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.CHE_COMPUTEWORK_REQ;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdABC#doExec()
	 */
	protected final IWsRespCmd doExec() {
		IWsRespCmd result = null;

		JsonNode persistentIdNode = getDataJsonNode().get("persistentId");
		String persistentId = persistentIdNode.getTextValue();
		Che che = mCheDao.findByPersistentId(UUID.fromString(persistentId));

		if (che != null) {
			JsonNode containerIdsNode = getDataJsonNode().get("containerIds");
			List<String> containerIdList = new ArrayList<String>();
			if (containerIdsNode.isArray()) {
				ArrayNode array = (ArrayNode) containerIdsNode;
				for (int i = 0; i < array.size(); i++) {
					JsonNode node = array.get(i);
					containerIdList.add(node.asText());
				}
			}
			result = new CheComputeWorkRespCmd(che, containerIdList);
		}
		return result;
	}
}
