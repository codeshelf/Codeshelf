package com.gadgetworks.codeshelf.ws.jetty.client;

import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;

public interface ResponseProcessor {
	void handleResponse(ResponseABC response);
}
