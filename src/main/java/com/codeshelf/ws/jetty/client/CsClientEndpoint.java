package com.codeshelf.ws.jetty.client;

import java.io.IOException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.ws.jetty.io.JsonDecoder;
import com.codeshelf.ws.jetty.io.JsonEncoder;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;

@ClientEndpoint(encoders = { JsonEncoder.class }, decoders = { JsonDecoder.class })
public class CsClientEndpoint {

	private static final Logger		LOGGER				= LoggerFactory.getLogger(CsClientEndpoint.class);

	private Counter	messageCounter;
	private Counter	sessionStartCounter;
	private Counter	sessionEndCounter;
	private Counter	sessionErrorCounter;

	@Setter
	static IMessageProcessor		messageProcessor;

	@Setter
	static MessageCoordinator		messageCoordinator;

	@Setter
	static JettyWebSocketClient	jettyWebsocketClient;

	public CsClientEndpoint() {
		IMetricsService metricsService = MetricsService.getInstance();
		messageCounter = metricsService.createCounter(MetricsGroup.WSS, "messages.received");
		sessionStartCounter	= metricsService.createCounter(MetricsGroup.WSS, "sessions.started");
		sessionEndCounter	= metricsService.createCounter(MetricsGroup.WSS, "sessions.ended");
		sessionErrorCounter	= metricsService.createCounter(MetricsGroup.WSS, "sessions.errors");
	}

	@OnOpen
	public final void onConnect(Session session) {
		sessionStartCounter.inc();
		LOGGER.info("Connected to server: " + session);
		jettyWebsocketClient.connected(session);
	}

    @OnMessage(maxMessageSize=JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
	public void onMessage(Session session, MessageABC message) throws IOException, EncodeException {
		messageCounter.inc();
		jettyWebsocketClient.messageReceived();
		if (message instanceof ResponseABC) {
			ResponseABC response = (ResponseABC) message;
			messageProcessor.handleResponse(null,response);
			messageCoordinator.unregisterRequest(response);

		} else if (message instanceof RequestABC) {
			RequestABC request = (RequestABC) message;
			LOGGER.debug("Request received: " + request);
			// pass request to processor to execute command
			ResponseABC response = messageProcessor.handleRequest(null, request);
			if (response != null) {
				// send response to client
				LOGGER.debug("Sending response " + response + " for request " + request);
				jettyWebsocketClient.sendMessage(response);
			} else {
				LOGGER.warn("No response generated for request " + request);
			}
		} else if (!(message instanceof KeepAlive)) {
			LOGGER.debug("Other message received: " + message);
			messageProcessor.handleMessage(null, message);
		}
	}

	@OnClose
	public final void onDisconnect(Session session, CloseReason reason) {
		sessionEndCounter.inc();
		LOGGER.info("Disconnected from server: " + reason);
		jettyWebsocketClient.disconnected(session);
	}

	@OnError
	public final void onError(Throwable cause) {
		sessionErrorCounter.inc();
		LOGGER.error("WebSocket: " + cause.getMessage());
	}
}
