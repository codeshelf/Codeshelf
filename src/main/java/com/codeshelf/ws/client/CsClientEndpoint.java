package com.codeshelf.ws.client;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.ws.io.JsonDecoder;
import com.codeshelf.ws.io.JsonEncoder;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.message.KeepAlive;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.google.inject.Inject;

@ClientEndpoint(encoders = { JsonEncoder.class }, decoders = { JsonDecoder.class })
public class CsClientEndpoint {

	private static final Logger		LOGGER				= LoggerFactory.getLogger(CsClientEndpoint.class);

	private Counter					messageCounter		= null;
	private Counter					sessionStartCounter	= null;
	private Counter					sessionEndCounter	= null;
	private Counter					sessionErrorCounter	= null;
	private IMetricsService			metricsService		= null;

	// static settable (reset between tests)
	@Setter
	static IMessageProcessor		messageProcessor;
	@Setter
	static WebSocketEventListener	eventListener;

	// instance injected 
	@Inject
	@Setter
	static MessageCoordinator		messageCoordinator;
	@Inject
	@Setter
	static WebSocketContainer		webSocketContainer;

	// generated / set on successful connection
	Session							session				= null;

	@Getter
	@Setter
	boolean							queueingEnabled		= true;
	MessageQueue					queue				= new MessageQueue();

	private URI						uri;

	@Getter
	@Setter
	long							lastMessageSent		= 0;

	@Getter
	@Setter
	long							lastMessageReceived	= 0;

	public CsClientEndpoint() {
		this.uri = URI.create(System.getProperty("websocket.uri"));
		if (this.uri == null) {
			String port = System.getProperty("api.port");
			if (port == null) {
				port = "12345";
			}
			this.uri = URI.create("ws://localhost:" + port + "/ws/");
		}

	}

	private void initMetrics() {
		if (metricsService == null) {
			metricsService = MetricsService.getInstance();
			messageCounter = metricsService.createCounter(MetricsGroup.WSS, "messages.received");
			sessionStartCounter = metricsService.createCounter(MetricsGroup.WSS, "sessions.started");
			sessionEndCounter = metricsService.createCounter(MetricsGroup.WSS, "sessions.ended");
			sessionErrorCounter = metricsService.createCounter(MetricsGroup.WSS, "sessions.errors");
		}
	}

	@OnOpen
	public final void onConnect(Session session) {
		initMetrics();

		if (sessionStartCounter != null)
			sessionStartCounter.inc();
		LOGGER.info("Connected to server: " + session);
		// set session object
		this.session = session;

		// register event with listener (device manager)
		if (eventListener != null)
			eventListener.connected();

		// send queued messages
		while (this.queue.getQueueLength() > 0) {
			MessageABC message = this.queue.peek();
			if (this.sendMessage(message)) {
				// remove from queue if sent
				this.queue.remove(message);
			} else {
				LOGGER.warn("Failed to send queued message #" + message.getMessageId());
			}
		}
	}

	@OnMessage(maxMessageSize = JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
	public void onMessage(Session session, MessageABC message) throws IOException, EncodeException {
		messageReceived();
		if (message instanceof ResponseABC) {
			ResponseABC response = (ResponseABC) message;
			messageProcessor.handleResponse(null, response);
			messageCoordinator.unregisterRequest(response);

		} else if (message instanceof RequestABC) {
			RequestABC request = (RequestABC) message;
			LOGGER.debug("Request received: " + request);
			// pass request to processor to execute command
			ResponseABC response = messageProcessor.handleRequest(null, request);
			if (response != null) {
				// send response to client
				LOGGER.debug("Sending response " + response + " for request " + request);
				sendMessage(response);
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
		if (sessionEndCounter != null)
			sessionEndCounter.inc();
		LOGGER.info("Disconnecting from server: " + reason);
		try {
			disconnect();
		} catch (IOException e) {
			LOGGER.error("Unexpected exception disconnecting from server", e);
		}
	}

	public void connect() throws DeploymentException, IOException {
		LOGGER.info("Connecting to WS server at " + uri);
		// connect to the server
		Session session = webSocketContainer.connectToServer(this, uri);
		if (session.isOpen()) {
			LOGGER.debug("Connected to WS server");
		} else {
			LOGGER.warn("Failed to start session on " + uri);
		}
	}

	public synchronized void disconnect() throws IOException {
		if (session != null) {
			Session closingSession = this.session;
			this.session = null;

			LOGGER.debug("closing session");

			CloseReason reason = new CloseReason(CloseCodes.NORMAL_CLOSURE, "Connection closed by client");
			closingSession.close(reason);

			Set<Session> sessions = closingSession.getOpenSessions();
			for (Session s : sessions) {
				if (s.isOpen() && !closingSession.equals(s)) {
					LOGGER.warn("unexpected session open after close()");
					s.close(reason);
				}
			}

			//fire disconnected to listeners
			if (eventListener != null)
				eventListener.disconnected();

		} else {
			LOGGER.debug("disconnecting client requested, but there is no session to close");
		}
	}

	@OnError
	public final void onError(Throwable cause) {
		if (sessionErrorCounter != null)
			sessionErrorCounter.inc();
		// LOGGER.error("WebSocket: " + cause.getMessage());
		// Saw this with a cause being a NPE, but no stack trace available. Error was just swallowed.
		LOGGER.error("WebSocket errorClass:{} message:{}", cause.getClass().getSimpleName(), cause.getMessage());
		// To track down add java exception breakpoint of the class that is logged, and reproduce.
	}

	public boolean isConnected() {
		if (session == null || !session.isOpen()) {
			return false;
		}
		return true;
	}

	public void messageReceived() {
		if (messageCounter != null)
			messageCounter.inc();
		this.lastMessageReceived = System.currentTimeMillis();
	}

	public void messageSent() {
		this.lastMessageSent = System.currentTimeMillis();
	}

	public boolean sendMessage(MessageABC message) {
		try {
			if (!isConnected()) {
				if (!this.queueingEnabled) {
					// unable to send message...
					LOGGER.error("Unable to send message " + message + ": Not connected to server");
					return false;
				} else {
					// attempt to queue message
					if (this.queue.addMessage(message)) {
						LOGGER.warn("Not connected to server. Message " + message + " queued.");
						return true;
					} else {
						LOGGER.error("Unable to send message " + message + ": Not connected to server and queueing failed");
						return false;
					}
				}
			}
			session.getBasicRemote().sendObject(message);
			this.messageSent();
			if (message instanceof RequestABC) {
				// keep track of request
				CsClientEndpoint.messageCoordinator.registerRequest((RequestABC) message);
			}
			return true;
		} catch (Exception e) {
			LOGGER.error("Exception while trying to send message #" + message.getMessageId(), e);
			try {
				this.disconnect();
			} catch (IOException ioe) {
				LOGGER.debug("IOException during disconnect", ioe);
			}
			return false;
		}
	}

}
