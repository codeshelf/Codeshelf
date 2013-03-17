/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CompleteWorkInstructionWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.io.IOException;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: CHE_WICOMP_REQ,
 * 	data {
 * 		persistentId: 	<persistentId>,
 * 		wi: 			<workInstruction>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class CompleteWorkInstructionWsReqCmd extends WsReqCmdABC {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(CompleteWorkInstructionWsReqCmd.class);

	private ITypedDao<Che>				mCheDao;
	private ITypedDao<WorkInstruction>	mWorkInstructionDao;

	public CompleteWorkInstructionWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Che> inCheDao, final ITypedDao<WorkInstruction> inWorkInstructionDao) {
		super(inCommandId, inDataNodeAsJson);
		mCheDao = inCheDao;
		mWorkInstructionDao = inWorkInstructionDao;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.CHE_WICOMPLETE_REQ;
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
			ObjectMapper mapper = new ObjectMapper();
			JsonNode wiNode = getDataJsonNode().get("wi");
			try {
				WorkInstruction wi = mapper.readValue(wiNode, WorkInstruction.class);
				
				WorkInstruction storedWi = mWorkInstructionDao.findByPersistentId(wi.getPersistentId());
				if (storedWi != null) {
					storedWi.setPickerId(wi.getPickerId());
					storedWi.setActualQuantity(wi.getActualQuantity());
					storedWi.setContainerId(wi.getContainerId());
					storedWi.setStatusEnum(wi.getStatusEnum());
					storedWi.setTypeEnum(WorkInstructionTypeEnum.ACTUAL);
					try {
						mWorkInstructionDao.store(storedWi);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}
			} catch (IOException e) {
				LOGGER.error("", e);
			}

			//result = new CheWorkWsRespCmd(che, locationId, containerIdList);
		}
		return result;
	}
}
