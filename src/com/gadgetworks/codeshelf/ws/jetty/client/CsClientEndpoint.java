package com.gadgetworks.codeshelf.ws.jetty.client;

import java.io.IOException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;

@ClientEndpoint(encoders = { JsonEncoder.class }, decoders = { JsonDecoder.class })
public class CsClientEndpoint {

	private static final Logger		LOGGER				= LoggerFactory.getLogger(CsClientEndpoint.class);

	private static final Counter	messageCounter		= MetricsService.addCounter(MetricsGroup.WSS, "messages.received");
	private static final Counter	sessionStartCounter	= MetricsService.addCounter(MetricsGroup.WSS, "sessions.started");
	private static final Counter	sessionEndCounter	= MetricsService.addCounter(MetricsGroup.WSS, "sessions.ended");
	private static final Counter	sessionErrorCounter	= MetricsService.addCounter(MetricsGroup.WSS, "sessions.errors");

	@Getter
	@Setter
	private MessageProcessor		messageProcessor;

	@Getter
	@Setter
	private SessionManager			sessionManager;

	@Getter
	@Setter
	private MessageCoordinator		messageCoordinator;

	private JettyWebSocketClient	client;

	public CsClientEndpoint(JettyWebSocketClient client) {
		this.client = client;
	}

	@OnOpen
	public final void onConnect(Session session) {
		sessionStartCounter.inc();
		LOGGER.info("Connected to server: " + session);
		client.connected(session);
	}

    @OnMessage(maxMessageSize=JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
	public void onMessage(Session session, MessageABC message) throws IOException, EncodeException {
		messageCounter.inc();
		client.messageReceived();
		if (message instanceof ResponseABC) {
			ResponseABC response = (ResponseABC) message;
			// check CS session
			UserSession csSession = sessionManager.getSession(session);
			if (csSession == null) {
				LOGGER.warn("No matching CS session found for session " + session.getId());
			}
			// null session ok here?
			messageProcessor.handleResponse(csSession, response);
			this.messageCoordinator.unregisterRequest(response);

		} else if (message instanceof RequestABC) {
			RequestABC request = (RequestABC) message;
			LOGGER.debug("Request received: " + request);
			// pass request to processor to execute command
			UserSession csSession = sessionManager.getSession(session);
			ResponseABC response = messageProcessor.handleRequest(csSession, request);
			if (response != null) {
				// send response to client
				LOGGER.debug("Sending response " + response + " for request " + request);
				client.sendMessage(response);
			} else {
				LOGGER.warn("No response generated for request " + request);
			}
		} else if (!(message instanceof KeepAlive)) {
			LOGGER.debug("Other message received: " + message);
			UserSession csSession = sessionManager.getSession(session);
			if (csSession == null) {
				LOGGER.warn("No matching CS session found for session " + session.getId());
			}
			// null session ok here?
			messageProcessor.handleOtherMessage(csSession, message);
		}
	}

	@OnClose
	public final void onDisconnect(CloseReason reason) {
		sessionEndCounter.inc();
		LOGGER.info("Disconnected from server: " + reason);
		client.disconnected();
	}

	@OnError
	public final void onError(Throwable cause) {
		sessionErrorCounter.inc();
		LOGGER.error("WebSocket: " + cause.getMessage());
	}
}
