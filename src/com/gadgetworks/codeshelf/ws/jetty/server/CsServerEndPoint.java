package com.gadgetworks.codeshelf.ws.jetty.server;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonRequestDecoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonResponseEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

@ServerEndpoint(value="/",encoders={JsonResponseEncoder.class},decoders={JsonRequestDecoder.class})
public class CsServerEndPoint {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsServerEndPoint.class);

	RequestProcessor mRequestProcessor;
	SessionManager mSessionManager;
	
	MetricRegistry metricsRegistry;
    
	public CsServerEndPoint() {	
		mSessionManager = SessionManager.getInstance();
		mRequestProcessor = RequestProcessorFactory.getInstance();
	}
	
	@OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
		LOGGER.info("WS Session Started: " + session.getId());
		mSessionManager.sessionStarted(session);
    }
    
    @OnMessage
    public void onMessage(Session session, RequestABC request) throws IOException, EncodeException {
        LOGGER.debug("Received request on session "+session.getId()+": " + request);
        // pass request to processor to execute command
        CsSession csSession = this.getCsSession(session);
        ResponseABC response = mRequestProcessor.handleRequest(csSession,request);
        if (response!=null) {
        	// send response to client
        	LOGGER.debug("Sending response "+response+" for request "+request);
        	session.getBasicRemote().sendObject(response);
        }
        else {
        	LOGGER.warn("No response generated for request "+request);
        }
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason) {
    	LOGGER.info(String.format("WS Session %s closed because of %s", session.getId(), reason));
		mSessionManager.sessionEnded(session);
    }
    
    @OnError
    public void onError(Session session, Throwable cause) {
    	LOGGER.error("WebSocket error", cause);
    }
    
    private CsSession getCsSession(Session session) {
    	return mSessionManager.getSession(session);
    }
}