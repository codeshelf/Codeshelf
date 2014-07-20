package com.gadgetworks.codeshelf.ws.jetty.client;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class MessageCoordinator {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(MessageCoordinator.class);

	// keeps track of requests that have been sent without a response yet
	LinkedHashMap<String, RequestInfo> requests = new LinkedHashMap<String, RequestInfo>();
	
	public boolean registerRequest(RequestABC request) {
		String requestId = request.getMessageId();
		if (requestId==null) {
			LOGGER.warn("Unable to register request. Request ID is not defined.");
			return false;
		}
		if (requests.containsKey(requestId)) {
			LOGGER.warn("Unable to register request #"+requestId+": Request with same ID exists.  Ignoring message");
			return false;
		}
		RequestInfo ri = new RequestInfo(request);
		this.requests.put(request.getMessageId(), ri);
		LOGGER.debug("Request #"+requestId+" registered");
		return true;
	}
	
	public boolean unregisterRequest(ResponseABC response) {
		String requestId = response.getRequestId();
		if (requestId==null) {
			LOGGER.warn("Unable to unregister request. Request ID is not defined in response.");
			return false;
		}
		RequestInfo ri = this.requests.get(requestId);
		if (ri==null) {
			LOGGER.warn("Unable to unregister request. Unable to find request #"+requestId);
			return false;
		}
		this.requests.remove(requestId);
		long elapsed = System.currentTimeMillis() - ri.getCreationTime().getTime();
		LOGGER.debug("Request #"+requestId+" completed in "+elapsed+" ms");
		return true;
	}
	
	public RequestABC getRequest(String requestId) {
		RequestInfo ri = this.requests.get(requestId);
		if (ri==null) {
			return null;
		}
		return ri.getRequest();
	}
}
