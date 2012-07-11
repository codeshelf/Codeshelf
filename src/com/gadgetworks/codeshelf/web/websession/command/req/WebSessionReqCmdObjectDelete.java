/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectDelete.java,v 1.3 2012/07/11 07:15:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectDelete;

/**
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJECT_LISTENER_REQ,
 * 	data {
 *		className:		<class_name>,
 *		persistentId:	<persistentId>,
 * 	}
 * }
 *
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectDelete extends WebSessionReqCmdABC {

	private static final Log	LOGGER				= LogFactory.getLog(WebSessionReqCmdObjectDelete.class);

	private IDaoProvider		mDaoProvider;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectDelete(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.OBJECT_DELETE_REQ;
	}

	public final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {

			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode classNode = dataJsonNode.get(CLASSNAME);
			String className = classNode.getTextValue();
			if (!className.startsWith("com.gadgetworks.codeshelf.model.persist.")) {
				className = "com.gadgetworks.codeshelf.model.persist." + className;
			}
			JsonNode objectIdNode = dataJsonNode.get(PERSISTENT_ID);
			long objectIdId = objectIdNode.getLongValue();

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (PersistABC.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				ITypedDao<PersistABC> dao = mDaoProvider.getDaoInstance((Class<PersistABC>) classObject);
				PersistABC object = dao.findByPersistentId(objectIdId);

				// Execute the "set" method against the parents to return the children.
				// (The method *must* start with "set" to ensure other methods don't get called.)
				if (object != null) {
					try {
						dao.delete(object);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}

					// Convert the list of objects into a JSon object.
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					JsonNode searchListNode = mapper.valueToTree(object);
					dataNode.put(RESULTS, searchListNode);

					result = new WebSessionRespCmdObjectDelete(dataNode);
				}
			}

		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		} catch (SecurityException e) {
			LOGGER.error("", e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		}

		return result;
	}
}
