package com.gadgetworks.codeshelf.ws.jetty.server;

import com.gadgetworks.codeshelf.ws.jetty.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;

public interface RequestProcessor {
	ResponseABC handleRequest(CsSession session, RequestABC request);
}
