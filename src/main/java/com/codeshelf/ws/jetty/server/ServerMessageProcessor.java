package com.codeshelf.ws.jetty.server;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.protocol.command.CommandABC;
import com.codeshelf.ws.jetty.protocol.command.CompleteWorkInstructionCommand;
import com.codeshelf.ws.jetty.protocol.command.ComputeDetailWorkCommand;
import com.codeshelf.ws.jetty.protocol.command.ComputeWorkCommand;
import com.codeshelf.ws.jetty.protocol.command.CreatePathCommand;
import com.codeshelf.ws.jetty.protocol.command.EchoCommand;
import com.codeshelf.ws.jetty.protocol.command.GetWorkCommand;
import com.codeshelf.ws.jetty.protocol.command.LoginCommand;
import com.codeshelf.ws.jetty.protocol.command.ObjectDeleteCommand;
import com.codeshelf.ws.jetty.protocol.command.ObjectGetCommand;
import com.codeshelf.ws.jetty.protocol.command.ObjectMethodCommand;
import com.codeshelf.ws.jetty.protocol.command.ObjectPropertiesCommand;
import com.codeshelf.ws.jetty.protocol.command.ObjectUpdateCommand;
import com.codeshelf.ws.jetty.protocol.command.RegisterFilterCommand;
import com.codeshelf.ws.jetty.protocol.command.ServiceMethodCommand;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.codeshelf.ws.jetty.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.jetty.protocol.request.ComputeWorkRequest;
import com.codeshelf.ws.jetty.protocol.request.CreatePathRequest;
import com.codeshelf.ws.jetty.protocol.request.DeviceRequest;
import com.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.codeshelf.ws.jetty.protocol.request.ObjectDeleteRequest;
import com.codeshelf.ws.jetty.protocol.request.ObjectGetRequest;
import com.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.codeshelf.ws.jetty.protocol.request.ObjectPropertiesRequest;
import com.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.codeshelf.ws.jetty.protocol.request.RegisterFilterRequest;
import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.protocol.request.ServiceMethodRequest;
import com.codeshelf.ws.jetty.protocol.response.FailureResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ServerMessageProcessor implements IMessageProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ServerMessageProcessor.class);
	
	private final Counter requestCounter;
	private final Counter responseCounter;
	private final Counter loginCounter;
	private final Counter missingResponseCounter;
	private final Counter echoCounter;
	private final Counter completeWiCounter;
	private final Counter computeWorkCounter;
	private final Counter getWorkCounter;
	private final Counter objectGetCounter;
	private final Counter objectUpdateCounter;
	private final Counter objectDeleteCounter;
	private final Counter objectPropertiesCounter;
	private final Counter objectFilterCounter;
	private final Counter keepAliveCounter;
	private final Counter applicationRequestCounter;
	private final Counter systemRequestCounter;
	private final Timer requestProcessingTimer;
	
	private ServiceFactory	serviceFactory;
	private ConvertUtilsBean	converter;

	private SessionManagerService	sessionManager;

	@Inject
	public ServerMessageProcessor(ServiceFactory serviceFactory, ConvertUtilsBean converter, SessionManagerService sessionManager) {
		LOGGER.debug("Creating "+this.getClass().getSimpleName());
		this.serviceFactory = serviceFactory;
		this.converter = converter;
		this.sessionManager = sessionManager;
		
		IMetricsService metricsService = MetricsService.getInstance();
		
		requestCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.processed");
		responseCounter = metricsService.createCounter(MetricsGroup.WSS,"responses.processed");
		loginCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.logins");
		missingResponseCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.missing-responses");
		echoCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.echo");
		completeWiCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.complete-workinstruction");
		computeWorkCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.compute-work");
		getWorkCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.get-work");
		objectGetCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.object-get");
		objectUpdateCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.object-update");
		objectDeleteCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.object-delete");
		objectPropertiesCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.object-properties");
		objectFilterCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.register-filter");
		keepAliveCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.keep-alive");
		applicationRequestCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.application");
		systemRequestCounter = metricsService.createCounter(MetricsGroup.WSS,"requests.system");
		requestProcessingTimer = metricsService.createTimer(MetricsGroup.WSS,"requests.processing-time");
		
	}
	
	ObjectChangeBroadcaster getObjectChangeBroadcaster() {
		return TenantPersistenceService.getInstance().getEventListenerIntegrator().getChangeBroadcaster();
	}
	
	@Override
	public ResponseABC handleRequest(UserSession csSession, RequestABC request) {
		LOGGER.info("Request received for processing: "+request);
		requestCounter.inc();
		CommandABC command = null;
		ResponseABC response = null;
		
        // process message...
    	final Timer.Context timerContext = requestProcessingTimer.time();

    	try {
			// TODO: get rid of message type handling using if statements and type casts...
			if (request instanceof LoginRequest) {
				LoginRequest loginRequest = (LoginRequest) request;
				command = new LoginCommand(csSession, loginRequest, getObjectChangeBroadcaster(), this.sessionManager);
				loginCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof EchoRequest) {
				command = new EchoCommand(csSession,(EchoRequest) request);
				echoCounter.inc();
			}		
			else if (request instanceof CompleteWorkInstructionRequest) {
				command = new CompleteWorkInstructionCommand(csSession,(CompleteWorkInstructionRequest) request, serviceFactory.getServiceInstance(WorkService.class));
				completeWiCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ComputeWorkRequest) {
				command = new ComputeWorkCommand(csSession,(ComputeWorkRequest) request, serviceFactory.getServiceInstance(WorkService.class));
				computeWorkCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ComputeDetailWorkRequest) {
				command = new ComputeDetailWorkCommand(csSession,(ComputeDetailWorkRequest) request, serviceFactory.getServiceInstance(WorkService.class));
				computeWorkCounter.inc();
				applicationRequestCounter.inc();
			}			
			else if (request instanceof GetWorkRequest) {
				command = new GetWorkCommand(csSession,(GetWorkRequest) request, serviceFactory.getServiceInstance(WorkService.class));
				getWorkCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ObjectGetRequest) {
				command = new ObjectGetCommand(csSession,(ObjectGetRequest) request);
				objectGetCounter.inc();
				applicationRequestCounter.inc();
			}			
			else if (request instanceof ObjectUpdateRequest) {
				command = new ObjectUpdateCommand(csSession,(ObjectUpdateRequest) request);
				objectUpdateCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ObjectDeleteRequest) {
				command = new ObjectDeleteCommand(csSession,(ObjectDeleteRequest) request);
				objectDeleteCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ObjectMethodRequest) {
				command = new ObjectMethodCommand(csSession,(ObjectMethodRequest) request);
				objectUpdateCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ObjectPropertiesRequest) {
				command = new ObjectPropertiesCommand(csSession,(ObjectPropertiesRequest) request);
				objectPropertiesCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof ServiceMethodRequest) {
				command = new ServiceMethodCommand(csSession,(ServiceMethodRequest) request, serviceFactory, converter);
				objectUpdateCounter.inc();
				applicationRequestCounter.inc();
			}
			else if (request instanceof RegisterFilterRequest) {
				command = new RegisterFilterCommand(csSession,(RegisterFilterRequest) request, getObjectChangeBroadcaster());
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
		        timerContext.stop();
			} else {
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
    	} catch (Exception e) {
    		response = new FailureResponse(ExceptionUtils.getMessage(e));
    		response.setRequestId(request.getMessageId());
    		if (request instanceof DeviceRequest) {
    			String cheId = ((DeviceRequest)request).getDeviceId();
    			((FailureResponse)response).setCheId(cheId);
    		}
	    } finally {
	    	if(timerContext != null)
	    		timerContext.stop();
	    }
	    // ...and return the response
		return response;
	}

	@Override
	public void handleResponse(UserSession session, ResponseABC response) {
		responseCounter.inc();
		LOGGER.warn("Unexpected response received on session "+session+": "+response);
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
