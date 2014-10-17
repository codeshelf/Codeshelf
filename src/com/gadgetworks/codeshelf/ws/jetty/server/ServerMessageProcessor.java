package com.gadgetworks.codeshelf.ws.jetty.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CommandABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CompleteWorkInstructionCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ComputeWorkCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CreatePathCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.EchoCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.GetWorkCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.LoginCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ObjectDeleteCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ObjectGetCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ObjectMethodCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ObjectUpdateCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.RegisterFilterCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.RegisterListenerCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ServiceMethodCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ComputeWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CreatePathRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectDeleteRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectGetRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterFilterRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RegisterListenerRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ServiceMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.google.inject.Inject;

public class ServerMessageProcessor extends MessageProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ServerMessageProcessor.class);
	
	private final Counter requestCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.processed");
	private final Counter responseCounter = MetricsService.addCounter(MetricsGroup.WSS,"responses.processed");
	private final Counter loginCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.logins");
//	private final Counter statusCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.network-status");
	private final Counter missingResponseCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.missing-responses");
	private final Counter echoCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.echo");
	private final Counter completeWiCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.complete-workinstruction");
	private final Counter computeWorkCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.compute-work");
	private final Counter getWorkCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.get-work");
	private final Counter objectGetCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.object-get");
	private final Counter objectUpdateCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.object-update");
	private final Counter objectDeleteCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.object-delete");
	private final Counter objectListenerCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.object-listener");
	private final Counter objectFilterCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.register-filter");
	private final Counter keepAliveCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.keep-alive");
	private final Counter applicationRequestCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.application");
	private final Counter systemRequestCounter = MetricsService.addCounter(MetricsGroup.WSS,"requests.system");
//	private final Meter requestMeter = MetricsService.addMeter(MetricsGroup.WSS,"requests.meter");
//	private final Meter responseMeter = MetricsService.addMeter(MetricsGroup.WSS,"responses.meter");
	private final Timer requestProcessingTimer = MetricsService.addTimer(MetricsGroup.WSS,"requests.processing-time");
//	private final Timer responseProcessingTimer = MetricsService.addTimer(MetricsGroup.WSS,"responses.processing-time");
	
	final private IDaoProvider daoProvider;

	final private WorkService	workService;
	
	@Inject
	public ServerMessageProcessor(IDaoProvider daoProvider) {
		LOGGER.debug("Creating "+this.getClass().getSimpleName());
		this.daoProvider = daoProvider;
		this.workService = new WorkService();
	}
	
	@Override
	public ResponseABC handleRequest(UserSession csSession, RequestABC request) {
		LOGGER.info("Request received for processing: "+request);
		requestCounter.inc();
		CommandABC command = null;
		ResponseABC response = null;

        // process message...
    	final Timer.Context context = requestProcessingTimer.time();

    	try {
			// TODO: get rid of message type handling using if statements and type casts...
			if (request instanceof LoginRequest) {
				LoginRequest loginRequest = (LoginRequest) request;
				command = new LoginCommand(csSession,loginRequest,daoProvider);
				loginCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof EchoRequest) {
				command = new EchoCommand(csSession,(EchoRequest) request);
				echoCounter.inc();
			}		
			else if (request instanceof CompleteWorkInstructionRequest) {
				command = new CompleteWorkInstructionCommand(csSession,(CompleteWorkInstructionRequest) request, this.workService);
				completeWiCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ComputeWorkRequest) {
				command = new ComputeWorkCommand(csSession,(ComputeWorkRequest) request);
				computeWorkCounter.inc();
				applicationRequestCounter.inc();
			}			
			else if (request instanceof GetWorkRequest) {
				command = new GetWorkCommand(csSession,(GetWorkRequest) request);
				getWorkCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ObjectGetRequest) {
				command = new ObjectGetCommand(csSession,(ObjectGetRequest) request);
				objectGetCounter.inc();
				applicationRequestCounter.inc();
			}			
			else if (request instanceof ObjectUpdateRequest) {
				command = new ObjectUpdateCommand(this.daoProvider, csSession,(ObjectUpdateRequest) request);
				objectUpdateCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ObjectDeleteRequest) {
				command = new ObjectDeleteCommand(this.daoProvider, csSession,(ObjectDeleteRequest) request);
				objectDeleteCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ObjectMethodRequest) {
				command = new ObjectMethodCommand(csSession,(ObjectMethodRequest) request);
				objectUpdateCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ServiceMethodRequest) {
				command = new ServiceMethodCommand(csSession,(ServiceMethodRequest) request);
				objectUpdateCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof RegisterListenerRequest) {
				command = new RegisterListenerCommand(csSession, (RegisterListenerRequest) request);
				objectListenerCounter.inc();
				applicationRequestCounter.inc();
			}			
			else if (request instanceof RegisterFilterRequest) {
				command = new RegisterFilterCommand(csSession,(RegisterFilterRequest) request);
				objectFilterCounter.inc();
				applicationRequestCounter.inc();
			}			
			else if (request instanceof CreatePathRequest) {
				command = new CreatePathCommand(csSession,(CreatePathRequest) request);
				objectFilterCounter.inc();
				applicationRequestCounter.inc();
			}			
			// check if matching command was found
			if (command==null) {
				LOGGER.warn("Unable to find matching command for request "+request+". Ignoring request.");
		        context.stop();
			} else {
				// inject context
				command.setDaoProvider(this.daoProvider);

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
			}

	    } finally {
	        context.stop();
	    }
	    // ...and return the response
		return response;
	}

	@Override
	public void handleResponse(UserSession session, ResponseABC response) {
		responseCounter.inc();
		LOGGER.warn("Unexpected response received on session "+session+": "+response);
		/*
		final Timer.Context context = responseProcessingTimer.time();
	    try {		
			if (response instanceof PingResponse) {
				PingResponse pingResponse = (PingResponse) response;
				if (csSession!=null) {
					csSession.pongReceived(pingResponse.getStartTime());
				}
				else {
					LOGGER.warn("Unable to set pong received data: Matching session not found.");
				}
			}
	    } finally {
	        context.stop();
	    }
		*/
	}

	@Override
	public void handleMessage(UserSession session, MessageABC message) {
		if (message instanceof KeepAlive) {
			keepAliveCounter.inc();
			systemRequestCounter.inc();
		}
		else {
			LOGGER.warn("Unexpected message received on session "+session+": "+message);
		}
	}
}
