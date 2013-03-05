/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdCheWork.java,v 1.1 2013/03/05 07:47:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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
 * 		cheId: <cheId>,
 * 		locationId: <locationId>,
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

		String authenticateResult = FAIL;

		JsonNode chedIdNode = getDataJsonNode().get("cheId");
		String cheId = chedIdNode.getTextValue();
		Che che = mCheDao.findByDomainId(null, cheId);

		if (che != null) {
			JsonNode locationIdNode = getDataJsonNode().get("facilityId");
			String locationId = locationIdNode.getTextValue();

			JsonNode containerIdsNode = getDataJsonNode().get("containerIds");
			ObjectMapper mapper = new ObjectMapper();
			try {
				List<String> containerIdList = mapper.readValue(containerIdsNode, new TypeReference<List<List>>() {
				});
				for (String containerId : containerIdList) {
					LOGGER.debug("Container ID: " + containerId);
				}
			} catch (JsonParseException e) {
				LOGGER.error("", e);
			} catch (JsonMappingException e) {
				LOGGER.error("", e);
			} catch (IOException e) {
				LOGGER.error("", e);
			}

			result = new WebSessionRespCmdCheWork();
		}
		return result;
	}
}
