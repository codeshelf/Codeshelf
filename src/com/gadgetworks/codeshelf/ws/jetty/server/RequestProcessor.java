package com.gadgetworks.codeshelf.ws.jetty.server;

import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public interface RequestProcessor {
	ResponseABC handleRequest(CsSession session, RequestABC request);
}
