package com.gadgetworks.codeshelf.ws.jetty.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CommandABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.EchoCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.LoginCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.NetworkAttachCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.NetworkStatusCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkAttachRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkStatusRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class CsRequestProcessor implements RequestProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsRequestProcessor.class);

	private final Counter requestCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.processed");
	private final Counter loginCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.logins");
	private final Counter attachCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.network-attach");
	private final Counter statusCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.network-status");
	private final Counter missingResponseCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.missing-responses");
	private final Counter echoCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.echo");
	private final Meter requestMeter = MetricsService.addMeter(MetricsGroup.WSS,"requests.meter-all");
	private final Timer requestProcessingTimer = MetricsService.addTimer(MetricsGroup.WSS,"requests.processing-time");
	
	public CsRequestProcessor() {
		LOGGER.debug("Creating "+this.getClass().getSimpleName());
	}
	
	@Override
	public ResponseABC handleRequest(CsSession session, RequestABC request) {
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
				command = new LoginCommand(session,loginRequest);
				loginCounter.inc();
			}
			else if (request instanceof EchoRequest) {
				EchoRequest echoRequest = (EchoRequest) request;
				command = new EchoCommand(session,echoRequest);
				echoCounter.inc();
			}		
			else if (request instanceof NetworkAttachRequest) {
				NetworkAttachRequest attachRequest = (NetworkAttachRequest) request;
				command = new NetworkAttachCommand(session,attachRequest);
				attachCounter.inc();
			}			
			else if (request instanceof NetworkStatusRequest) {
				NetworkStatusRequest updateRequest = (NetworkStatusRequest) request;
				command = new NetworkStatusCommand(session,updateRequest);
				statusCounter.inc();
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
				response.setRequestID(request.getRequestId());
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

}
