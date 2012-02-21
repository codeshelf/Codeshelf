/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandObjectQuery.java,v 1.3 2012/02/21 23:32:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class WebSessionCommandObjectQuery extends WebSessionCommandABC {

	private static final Log	LOGGER		= LogFactory.getLog(WebSessionCommandObjectQuery.class);

	private static final String	CLASS_NODE	= "class";
	private static final String	QUERY_NODE	= "query";
	private static final String OBJECT_RESULTS_NODE = "objects";

	private String				mObjectClass;
	private String				mQuery;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionCommandObjectQuery(final String inCommandId, final JsonNode inDataNodeAsJson) {
		super(inCommandId, inDataNodeAsJson);

		JsonNode dataJsonNode = getDataJsonNode();
		JsonNode classNode = dataJsonNode.get(CLASS_NODE);
		mObjectClass = classNode.getTextValue();

		JsonNode queryNode = dataJsonNode.get(QUERY_NODE);
		mQuery = queryNode.getTextValue();
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE;
	}

	public final IWebSessionCommand doExec() {
		IWebSessionCommand result = null;

		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.
		// (We should also defend against other, weird query constructs that may be trying to circumvent this restriction.)

		try {
			Class<? extends PersistABC> clazz = (Class<? extends PersistABC>) Class.forName(mObjectClass);
			Query<? extends PersistABC> query = Ebean.find(clazz);
			query.where(mQuery);

			List<? extends PersistABC> searchList = query.findList();

			// Convert the list of ojects into a JSon object.
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode dataNode = mapper.createObjectNode();
			//String results = mapper.writeValueAsString(searchList);
			//dataNode.put(OBJECT_RESULTS_NODE, results);
			ArrayNode searchListNode = mapper.valueToTree(searchList);
			//ArrayNode resultsNode = dataNode.putArray("objects");
			dataNode.put("objects", searchListNode);
			
			result = new WebSessionCommandObjectQueryResp(dataNode);

		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
//		} catch (JsonGenerationException e) {
//			LOGGER.error("", e);
//		} catch (JsonMappingException e) {
//			LOGGER.error("", e);
//		} catch (IOException e) {
//			LOGGER.error("", e);
		}

		return result;
	}

	protected final void prepareDataNode(ObjectNode inOutDataNode) {
		// TODO Auto-generated method stub
	}
}
