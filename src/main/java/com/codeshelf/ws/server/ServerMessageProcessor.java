package com.codeshelf.ws.server;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codeshelf.manager.Tenant;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.InventoryService;
import com.codeshelf.service.NotificationService;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.command.CommandABC;
import com.codeshelf.ws.protocol.command.CompleteWorkInstructionCommand;
import com.codeshelf.ws.protocol.command.ComputeDetailWorkCommand;
import com.codeshelf.ws.protocol.command.ComputePutWallInstructionCommand;
import com.codeshelf.ws.protocol.command.ComputeWorkCommand;
import com.codeshelf.ws.protocol.command.CreatePathCommand;
import com.codeshelf.ws.protocol.command.EchoCommand;
import com.codeshelf.ws.protocol.command.InventoryLightItemCommand;
import com.codeshelf.ws.protocol.command.InventoryLightLocationCommand;
import com.codeshelf.ws.protocol.command.InventoryUpdateCommand;
import com.codeshelf.ws.protocol.command.LoginCommand;
import com.codeshelf.ws.protocol.command.ObjectDeleteCommand;
import com.codeshelf.ws.protocol.command.ObjectGetCommand;
import com.codeshelf.ws.protocol.command.ObjectMethodCommand;
import com.codeshelf.ws.protocol.command.ObjectPropertiesCommand;
import com.codeshelf.ws.protocol.command.ObjectUpdateCommand;
import com.codeshelf.ws.protocol.command.PutWallPlacementCommand;
import com.codeshelf.ws.protocol.command.RegisterFilterCommand;
import com.codeshelf.ws.protocol.command.ServiceMethodCommand;
import com.codeshelf.ws.protocol.command.VerifyBadgeCommand;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.message.KeepAlive;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.codeshelf.ws.protocol.request.CompleteWorkInstructionRequest;
import com.codeshelf.ws.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.protocol.request.ComputePutWallInstructionRequest;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest;
import com.codeshelf.ws.protocol.request.CreatePathRequest;
import com.codeshelf.ws.protocol.request.DeviceRequest;
import com.codeshelf.ws.protocol.request.EchoRequest;
import com.codeshelf.ws.protocol.request.InventoryLightItemRequest;
import com.codeshelf.ws.protocol.request.InventoryLightLocationRequest;
import com.codeshelf.ws.protocol.request.InventoryUpdateRequest;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.request.ObjectDeleteRequest;
import com.codeshelf.ws.protocol.request.ObjectGetRequest;
import com.codeshelf.ws.protocol.request.ObjectMethodRequest;
import com.codeshelf.ws.protocol.request.ObjectPropertiesRequest;
import com.codeshelf.ws.protocol.request.ObjectUpdateRequest;
import com.codeshelf.ws.protocol.request.PutWallPlacementRequest;
import com.codeshelf.ws.protocol.request.RegisterFilterRequest;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.request.ServiceMethodRequest;
import com.codeshelf.ws.protocol.request.VerifyBadgeRequest;
import com.codeshelf.ws.protocol.response.FailureResponse;
import com.codeshelf.ws.protocol.response.PingResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ServerMessageProcessor implements IMessageProcessor {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(ServerMessageProcessor.class);

	private final Counter			requestCounter;
	private final Counter			responseCounter;
	private final Counter			loginCounter;
	private final Counter			missingResponseCounter;
	private final Counter			echoCounter;
	private final Counter			completeWiCounter;
	private final Counter			computeWorkCounter;
	private final Counter			objectGetCounter;
	private final Counter			objectUpdateCounter;
	private final Counter			objectDeleteCounter;
	private final Counter			objectPropertiesCounter;
	private final Counter			objectFilterCounter;
	private final Counter			keepAliveCounter;
	private final Counter			applicationRequestCounter;
	private final Counter			systemRequestCounter;
	private final Counter			inventoryUpdateRequestCounter;
	private final Counter			inventoryLightItemRequestCounter;
	private final Counter			inventoryLightLocationRequestCounter;
	private final Counter			putWallPlacementCounter;
	private final Counter			notificationCounter;
	private final Timer				requestProcessingTimer;

	private ServiceFactory			serviceFactory;
	private ConvertUtilsBean		converter;

	private WebSocketManagerService	sessionManager;

	@Inject
	public ServerMessageProcessor(ServiceFactory serviceFactory, ConvertUtilsBean converter, WebSocketManagerService sessionManager) {
		LOGGER.debug("Creating " + this.getClass().getSimpleName());
		this.serviceFactory = serviceFactory;
		this.converter = converter;
		this.sessionManager = sessionManager;

		IMetricsService metricsService = MetricsService.getInstance();

		requestCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.processed");
		responseCounter = metricsService.createCounter(MetricsGroup.WSS, "responses.processed");
		loginCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.logins");
		missingResponseCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.missing-responses");
		echoCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.echo");
		completeWiCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.complete-workinstruction");
		computeWorkCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.compute-work");
		objectGetCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.object-get");
		objectUpdateCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.object-update");
		objectDeleteCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.object-delete");
		objectPropertiesCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.object-properties");
		objectFilterCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.register-filter");
		keepAliveCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.keep-alive");
		applicationRequestCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.application");
		systemRequestCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.system");
		inventoryUpdateRequestCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.inventory-update");
		inventoryLightItemRequestCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.inventory-light-item");
		inventoryLightLocationRequestCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.inventory-light-location");
		putWallPlacementCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.put-wall-placement");
		requestProcessingTimer = metricsService.createTimer(MetricsGroup.WSS, "requests.processing-time");
		notificationCounter = metricsService.createCounter(MetricsGroup.WSS, "requests.notification");
	}

	ObjectChangeBroadcaster getObjectChangeBroadcaster() {
		return TenantPersistenceService.getInstance().getEventListenerIntegrator().getChangeBroadcaster();
	}

	@Override
	public ResponseABC handleRequest(WebSocketConnection csSession, RequestABC request) {
		LOGGER.info("Request received for processing: {}", request);

		requestCounter.inc();
		CommandABC command = null;
		ResponseABC response = null;

		// process message...
		final Timer.Context timerContext = requestProcessingTimer.time();
		UserContext user = csSession.getCurrentUserContext();
		Tenant tenant = csSession.getCurrentTenant();
		if (user == null && tenant != null) {
			throw new IllegalArgumentException("got request with tenant " + tenant.getId() + " but no user!");
		}
		// TODO: get rid of message type handling using if statements and type casts...
		if (request instanceof LoginRequest) {
			LoginRequest loginRequest = (LoginRequest) request;
			command = new LoginCommand(csSession, loginRequest, getObjectChangeBroadcaster(), this.sessionManager);
			loginCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof EchoRequest) {
			command = new EchoCommand(csSession, (EchoRequest) request);
			echoCounter.inc();
		} else if (request instanceof CompleteWorkInstructionRequest) {
			command = new CompleteWorkInstructionCommand(csSession,
				(CompleteWorkInstructionRequest) request,
				serviceFactory.getServiceInstance(WorkService.class));
			completeWiCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof VerifyBadgeRequest) {
			VerifyBadgeRequest req = (VerifyBadgeRequest) request;
			command = new VerifyBadgeCommand(csSession, req, serviceFactory.getServiceInstance(WorkService.class));
			applicationRequestCounter.inc();
		} else if (request instanceof ComputeWorkRequest) {
			ComputeWorkRequest req = (ComputeWorkRequest) request;
			command = new ComputeWorkCommand(csSession, req, serviceFactory.getServiceInstance(WorkService.class));
			computeWorkCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof ComputeDetailWorkRequest) {
			command = new ComputeDetailWorkCommand(csSession,
				(ComputeDetailWorkRequest) request,
				serviceFactory.getServiceInstance(WorkService.class));
			applicationRequestCounter.inc();
		} else if (request instanceof ComputePutWallInstructionRequest) {
			command = new ComputePutWallInstructionCommand(csSession,
				(ComputePutWallInstructionRequest) request,
				serviceFactory.getServiceInstance(WorkService.class));
			applicationRequestCounter.inc();
		} else if (request instanceof ObjectGetRequest) {
			command = new ObjectGetCommand(csSession, (ObjectGetRequest) request);
			objectGetCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof ObjectUpdateRequest) {
			command = new ObjectUpdateCommand(csSession, (ObjectUpdateRequest) request);
			objectUpdateCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof ObjectDeleteRequest) {
			command = new ObjectDeleteCommand(csSession, (ObjectDeleteRequest) request);
			objectDeleteCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof ObjectMethodRequest) {
			command = new ObjectMethodCommand(csSession, (ObjectMethodRequest) request);
			objectUpdateCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof ObjectPropertiesRequest) {
			command = new ObjectPropertiesCommand(csSession, (ObjectPropertiesRequest) request);
			objectPropertiesCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof ServiceMethodRequest) {
			command = new ServiceMethodCommand(csSession, (ServiceMethodRequest) request, serviceFactory, converter);
			objectUpdateCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof RegisterFilterRequest) {
			command = new RegisterFilterCommand(csSession, (RegisterFilterRequest) request, getObjectChangeBroadcaster());
			objectFilterCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof CreatePathRequest) {
			command = new CreatePathCommand(csSession, (CreatePathRequest) request);
			objectFilterCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof InventoryUpdateRequest) {
			command = new InventoryUpdateCommand(csSession,
				(InventoryUpdateRequest) request,
				serviceFactory.getServiceInstance(InventoryService.class));
			inventoryUpdateRequestCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof InventoryLightItemRequest) {
			command = new InventoryLightItemCommand(csSession,
				(InventoryLightItemRequest) request,
				serviceFactory.getServiceInstance(InventoryService.class));
			inventoryLightItemRequestCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof InventoryLightLocationRequest) {
			command = new InventoryLightLocationCommand(csSession,
				(InventoryLightLocationRequest) request,
				serviceFactory.getServiceInstance(InventoryService.class));
			inventoryLightLocationRequestCounter.inc();
			applicationRequestCounter.inc();
		} else if (request instanceof PutWallPlacementRequest) {
			command = new PutWallPlacementCommand(csSession,
				(PutWallPlacementRequest) request,
				serviceFactory.getServiceInstance(WorkService.class));
			putWallPlacementCounter.inc();
			applicationRequestCounter.inc();
		} else {
			LOGGER.error("invalid message {} for user {}", request.getClass().getSimpleName(), user.getUsername());
		}
		try {
			// check if matching command was found
			if (command == null) {
				LOGGER.warn("Unable to find matching command for request " + request + ". Ignoring request.");
				timerContext.stop();
			} else {
				// throw exception if not authorized
				CodeshelfSecurityManager.authorizeAnnotatedClass(command.getClass());

				// execute command and generate response to be sent to client
				response = command.exec();
				if (response != null) {
					// automatically tie response to request
					response.setRequestId(request.getMessageId());
				} else {
					LOGGER.warn("No response generated for request " + request);
					missingResponseCounter.inc();
				}
			}
		} catch (Exception e) {
			String message;
			if (e instanceof NullPointerException || e instanceof HibernateException) {
				message = "Exception"; // do not return detail on this type of exception to caller
				LOGGER.error("Unexpected exception in ServerMessageProcessor", e);
			} else {
				message = ExceptionUtils.getMessage(e);
				LOGGER.warn("Error processing {} request: {}", request.getClass().getSimpleName(), message);
			}
			response = new FailureResponse(message);
			response.setRequestId(request.getMessageId());
			if (request instanceof DeviceRequest) {
				String cheId = ((DeviceRequest) request).getDeviceId();
				((FailureResponse) response).setCheId(cheId);
			}
		} finally {
			if (timerContext != null)
				timerContext.stop();
		}
		// ...and return the response
		return response;
	}

	@Override
	public void handleResponse(WebSocketConnection session, ResponseABC response) {
		responseCounter.inc();
		if (response instanceof PingResponse) {
			PingResponse pingResponse = (PingResponse) response;
			if (session != null) {
				session.pongReceived(pingResponse.getStartTime());
			} else {
				LOGGER.warn("Unable to set pong received data: Matching session not found.");
			}
		} else {
			LOGGER.warn("Unexpected response received on session " + session + ": " + response);
		}
	}

	@Override
	public void handleMessage(WebSocketConnection session, MessageABC message) {
		if (message instanceof KeepAlive) {
			keepAliveCounter.inc();
			systemRequestCounter.inc();
		} else if (message instanceof NotificationMessage) {
			notificationCounter.inc();
			NotificationService service = serviceFactory.getServiceInstance(NotificationService.class);
			service.saveEvent((NotificationMessage) message);
		} else {
			LOGGER.warn("Unexpected message received on session " + session + ": " + message);
		}
	}
}
