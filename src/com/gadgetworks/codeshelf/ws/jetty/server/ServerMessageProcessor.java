package com.gadgetworks.codeshelf.ws.jetty.server;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CommandABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CompleteWorkInstructionCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ComputeWorkCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.EchoCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.GetWorkCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.LoginCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.NetworkAttachCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.NetworkStatusCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ComputeWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkAttachRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkStatusRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.PingResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class ServerMessageProcessor extends MessageProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ServerMessageProcessor.class);

	private final Counter requestCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.processed");
	private final Counter responseCounter = MetricsService.addCounter(MetricsGroup.WSS,"responses.processed");
	private final Counter loginCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.logins");
	private final Counter attachCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.network-attach");
	private final Counter statusCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.network-status");
	private final Counter missingResponseCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.missing-responses");
	private final Counter echoCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.echo");
	private final Counter completeWiCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.complete-workinstruction");
	private final Counter computeWorkCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.compute-work");
	private final Counter getWorkCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.get-work");
	private final Meter requestMeter = MetricsService.addMeter(MetricsGroup.WSS,"requests.meter");
	private final Meter responseMeter = MetricsService.addMeter(MetricsGroup.WSS,"responses.meter");
	private final Timer requestProcessingTimer = MetricsService.addTimer(MetricsGroup.WSS,"requests.processing-time");
	private final Timer responseProcessingTimer = MetricsService.addTimer(MetricsGroup.WSS,"responses.processing-time");
	private final Histogram pingHistogram = MetricsService.addHistogram(MetricsGroup.WSS, "ping-histogram");
	
	SessionManager sessionManager = SessionManager.getInstance();
	
	public ServerMessageProcessor() {
		LOGGER.debug("Creating "+this.getClass().getSimpleName());
	}
	
	@Override
	public ResponseABC handleRequest(Session session, RequestABC request) {
		LOGGER.info("Request received for processing: "+request);
		CommandABC command = null;
		ResponseABC response = null;
		
		requestCounter.inc();
		requestMeter.mark();
		
    	final Timer.Context context = requestProcessingTimer.time();
	    try {
			// TODO: consider changing to declarative implementation using custom annotation
			// to get rid of handling via if statements and type casts...
			if (request instanceof LoginRequest) {
				LoginRequest loginRequest = (LoginRequest) request;
	            CsSession csSession = sessionManager.getSession(session);
				command = new LoginCommand(csSession,loginRequest);
				loginCounter.inc();
			}
			else if (request instanceof EchoRequest) {
				command = new EchoCommand((EchoRequest) request);
				echoCounter.inc();
			}		
			else if (request instanceof NetworkAttachRequest) {
				command = new NetworkAttachCommand((NetworkAttachRequest) request);
				attachCounter.inc();
			}			
			else if (request instanceof NetworkStatusRequest) {
				command = new NetworkStatusCommand((NetworkStatusRequest) request);
				statusCounter.inc();
			}			
			else if (request instanceof CompleteWorkInstructionRequest) {
				command = new CompleteWorkInstructionCommand((CompleteWorkInstructionRequest) request);
				completeWiCounter.inc();
			}
			else if (request instanceof ComputeWorkRequest) {
				command = new ComputeWorkCommand((ComputeWorkRequest) request);
				computeWorkCounter.inc();
			}			
			else if (request instanceof GetWorkRequest) {
				command = new GetWorkCommand((GetWorkRequest) request);
				getWorkCounter.inc();
			}			
			// check if matching command was found
			if (command==null) {
				LOGGER.warn("Unable to find matching command for request "+request+". Ignoring request.");
		        context.stop();
				return null;
			}
	
			// execute command and generate response to be sent to client
			response = command.exec();
			if (response!=null) {
				// automatically tie response to request
				response.setRequestId(request.getMessageId());
			}
			else {
				LOGGER.warn("No response generated for request "+request);
				missingResponseCounter.inc();
			}
	    } finally {
	        context.stop();
	    }
		return response;
	}

	@Override
	public void handleResponse(Session session, ResponseABC response) {
		responseCounter.inc();
		responseMeter.mark();
    	final Timer.Context context = responseProcessingTimer.time();
	    try {		
			if (response instanceof PingResponse) {
				PingResponse pingResponse = (PingResponse) response;
				long now = System.currentTimeMillis();
				long delta = now-pingResponse.getStartTime();
				pingHistogram.update(delta);
				double elapsedSec = ((double) delta)/1000; 
				LOGGER.debug("Ping roundtrip on session "+session.getId()+" in "+elapsedSec+"s");
			}
	    } finally {
	        context.stop();
	    }
	}
}
