/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdCheWork.java,v 1.3 2013/03/07 05:23:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdCheWork;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: CHE_WORK_REQ,
 * 	data {
 * 		cheId: 			<cheId>,
 * 		persistentId: 	<persistentId>,
 * 		locationId: 	<locationId>,
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
public class WebSessionReqCmdCheWork extends WebSessionReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WebSessionReqCmdCheWork.class);

	private ITypedDao<Che>		mCheDao;

	public WebSessionReqCmdCheWork(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Che> inCheDao) {
		super(inCommandId, inDataNodeAsJson);
		mCheDao = inCheDao;
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.CHE_WORK_REQ;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdABC#doExec()
	 */
	protected final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		JsonNode chedIdNode = getDataJsonNode().get("cheId");
		String cheId = chedIdNode.getTextValue();

		JsonNode persistentIdNode = getDataJsonNode().get("persistentId");
		String persistentId = persistentIdNode.getTextValue();
		Che che = mCheDao.findByPersistentId(UUID.fromString(persistentId));

		if (che != null) {
			JsonNode locationIdNode = getDataJsonNode().get("locationId");
			String locationId = locationIdNode.getTextValue();

			JsonNode containerIdsNode = getDataJsonNode().get("containerIds");
			List<String> containerIdList = new ArrayList<String>();
			if (containerIdsNode.isArray()) {
				ArrayNode array = (ArrayNode) containerIdsNode;
				for (int i = 0; i < array.size(); i++) {
					JsonNode node = array.get(i);
					containerIdList.add(node.asText());
				}
			}
			result = new WebSessionRespCmdCheWork(che, locationId, containerIdList);
		}
		return result;
	}
}
