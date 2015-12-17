package com.codeshelf.ws.client;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
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
import com.codahale.metrics.Meter;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.io.JsonDecoder;
import com.codeshelf.ws.io.JsonEncoder;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.message.KeepAlive;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.request.DeviceRequestABC;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.ComputeWorkResponse;
import com.codeshelf.ws.protocol.response.GetOrderDetailWorkResponse;
import com.codeshelf.ws.protocol.response.GetPutWallInstructionResponse;
import com.codeshelf.ws.protocol.response.InfoResponse;
import com.codeshelf.ws.protocol.response.LinkRemoteCheResponse;
import com.codeshelf.ws.protocol.response.PalletizerItemResponse;
import com.codeshelf.ws.protocol.response.PalletizerRemoveOrderResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.TapeLocationDecodingResponse;
import com.codeshelf.ws.protocol.response.VerifyBadgeResponse;
import com.google.inject.Inject;

@ClientEndpoint(encoders = { JsonEncoder.class }, decoders = { JsonDecoder.class })
public class CsClientEndpoint {

	private static final Logger		LOGGER				= LoggerFactory.getLogger(CsClientEndpoint.class);

	private static final int		DEFAULT_RECONNECT_DELAY_MS	= 30000;

	private Counter					messageCounter		= null;
	private Counter					sessionStartCounter	= null;
	private Counter					sessionEndCounter	= null;
	private Counter					sessionErrorCounter	= null;
	private Meter					verifyBadgeMeter 	= null;
	private Meter					computeWorkMeter 	= null;
	//private Meter					getWorkMeter		= null;
	private Meter					orderDetailWorkMeter= null;
	private Meter					putWallInstructionMeter	= null;
	private Meter					tapeDecodingMeter	= null;
	private Meter					infoMeter			= null;
	private Meter					palletizerItemMeter	= null;
	private Meter					palletizerRemoveMeter	= null;
	private Meter					linkRemoteChe		= null;
	private IMetricsService			metricsService		= null;
	
	@SuppressWarnings("rawtypes")
	private HashMap<Class, Meter>	classToMeterHash	= new HashMap<>();

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
	
	private long					lastConnectionAttempt = 0;	
	private int						reconnectDelayMs = 0;
	
	private HashMap<String, Long>	requestTimes	= new HashMap<>();

	
	public CsClientEndpoint() {
		this.uri = URI.create(System.getProperty("websocket.uri"));
		if (this.uri == null) {
			String port = System.getProperty("api.port");
			if (port == null) {
				port = "12345";
			}
			this.uri = URI.create("ws://localhost:" + port + "/ws/");
		}
		this.reconnectDelayMs = Integer.getInteger("sitecontroller.reconnectdelayms", CsClientEndpoint.DEFAULT_RECONNECT_DELAY_MS);
	}

	private void initMetrics() {
		if (metricsService == null) {
			metricsService = MetricsService.getInstance();
			messageCounter			= metricsService.createCounter(MetricsGroup.WSS, "messages.received");
			sessionStartCounter		= metricsService.createCounter(MetricsGroup.WSS, "sessions.started");
			sessionEndCounter		= metricsService.createCounter(MetricsGroup.WSS, "sessions.ended");
			sessionErrorCounter		= metricsService.createCounter(MetricsGroup.WSS, "sessions.errors");
			verifyBadgeMeter		= metricsService.createMeter(MetricsGroup.WSS, "responses.verify-badge");
			computeWorkMeter		= metricsService.createMeter(MetricsGroup.WSS, "responses.compute-work");
			//getWorkMeter			= metricsService.createMeter(MetricsGroup.WSS, "responses.get-work");
			orderDetailWorkMeter	= metricsService.createMeter(MetricsGroup.WSS, "responses.order-detail-work");
			putWallInstructionMeter	= metricsService.createMeter(MetricsGroup.WSS, "responses.put-wall-instruction");
			tapeDecodingMeter		= metricsService.createMeter(MetricsGroup.WSS, "responses.tape-decoding");
			infoMeter				= metricsService.createMeter(MetricsGroup.WSS, "responses.info");
			palletizerItemMeter		= metricsService.createMeter(MetricsGroup.WSS, "responses.palletizer-item");
			palletizerRemoveMeter	= metricsService.createMeter(MetricsGroup.WSS, "responses.palletizer-remove");
			linkRemoteChe			= metricsService.createMeter(MetricsGroup.WSS, "responses.link-remote-che");
			
			classToMeterHash.put(VerifyBadgeResponse.class, verifyBadgeMeter);
			classToMeterHash.put(ComputeWorkResponse.class, computeWorkMeter);
			classToMeterHash.put(GetOrderDetailWorkResponse.class, orderDetailWorkMeter);
			classToMeterHash.put(GetPutWallInstructionResponse.class, putWallInstructionMeter);
			classToMeterHash.put(TapeLocationDecodingResponse.class, tapeDecodingMeter);
			classToMeterHash.put(InfoResponse.class, infoMeter);
			classToMeterHash.put(PalletizerItemResponse.class, palletizerItemMeter);
			classToMeterHash.put(PalletizerRemoveOrderResponse.class, palletizerRemoveMeter);
			classToMeterHash.put(LinkRemoteCheResponse.class, linkRemoteChe);
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
		try {
			
			messageReceived();
			if (message instanceof ResponseABC) {
				ResponseABC response = (ResponseABC) message;
				messageProcessor.handleResponse(null, response);
				messageCoordinator.unregisterRequest(response);
				updateResponseMetrics((ResponseABC)message);
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
		finally {
			
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
	
	private void updateResponseMetrics(ResponseABC response){
		Long requestTime = requestTimes.remove(response.getRequestId());
		long duration = 0;
		if (requestTime != null){
			duration = System.currentTimeMillis() - requestTime;
		} else {
			LOGGER.error("Could not find request time for {}. Saving duration 0.", response.getClass().getName());
		}
		Meter meter = classToMeterHash.get(response.getClass());
		if (meter != null) {
			LOGGER.info("Logging duration {} for response {}", duration, response.getClass().getSimpleName());
			meter.mark(duration);
		}
	}

	public void connect() throws DeploymentException, IOException {
		// make sure we are not rapidly reconnecting to server
		long now = System.currentTimeMillis();
		long delay = (this.lastConnectionAttempt+this.reconnectDelayMs) - now; 
		if(delay>0) {
			LOGGER.info("Waiting " + Long.toString(delay/1000) + " seconds before reconnecting.");
			ThreadUtils.sleep((int)delay);
		}
		this.lastConnectionAttempt = System.currentTimeMillis();

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
		LOGGER.error("WebSocket unhandled exception", cause);
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
			if (message instanceof RequestABC) {
				// keep track of request
				CsClientEndpoint.messageCoordinator.registerRequest((RequestABC) message);
				
				if (message instanceof DeviceRequestABC) {
					LOGGER.info("Request sent: {}", message);
				}
			}
			requestTimes.put(message.getMessageId(), System.currentTimeMillis());
			session.getBasicRemote().sendObject(message);
			this.messageSent();
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
