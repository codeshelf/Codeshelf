package com.codeshelf.api.resources.subresources;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Setter;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.EventTypeParam;
import com.codeshelf.api.BaseResponse.TimestampParam;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.HardwareRequest;
import com.codeshelf.api.HardwareRequest.CheDisplayRequest;
import com.codeshelf.api.HardwareRequest.LightRequest;
import com.codeshelf.api.pickscript.ScriptParser;
import com.codeshelf.api.pickscript.ScriptParser.ScriptStep;
import com.codeshelf.api.pickscript.ScriptServerRunner;
import com.codeshelf.api.pickscript.ScriptSiteCallPool;
import com.codeshelf.api.pickscript.ScriptStepParser;
import com.codeshelf.api.pickscript.ScriptStepParser.StepPart;
import com.codeshelf.api.resources.ExtensionPointsResource;
import com.codeshelf.api.resources.OrdersResource;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.api.responses.ItemDisplay;
import com.codeshelf.api.responses.PickRate;
import com.codeshelf.api.responses.ResultDisplay;
import com.codeshelf.api.responses.WorkerDisplay;
import com.codeshelf.behavior.BatchProcessor;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.NotificationBehavior.WorkerEventTypeGroup;
import com.codeshelf.behavior.OrderBehavior;
import com.codeshelf.behavior.ProductivitySummaryList;
import com.codeshelf.behavior.TenantCallable;
import com.codeshelf.behavior.TestBehavior;
import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedInstrListMessage;
import com.codeshelf.device.LedSample;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.ExtensionPointService;
import com.codeshelf.service.ParameterSetBeanABC;
import com.codeshelf.service.PropertyService;
import com.codeshelf.ws.protocol.message.CheDisplayMessage;
import com.codeshelf.ws.protocol.message.LightLedsInstruction;
import com.codeshelf.ws.protocol.message.ScriptMessage;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.multipart.FormDataMultiPart;

public class FacilityResource {

	private static final Logger	LOGGER = LoggerFactory.getLogger(FacilityResource.class);

	
	private final WorkBehavior	workService;
	private final OrderBehavior orderService;
	private final NotificationBehavior notificationService;
	private final WebSocketManagerService webSocketManagerService;
	private final UiUpdateBehavior uiUpdateService;
	private final PropertyService propertyService;
	private final Provider<ICsvAislesFileImporter> aislesImporterProvider;
	private final Provider<ICsvLocationAliasImporter> locationsImporterProvider;
	private final Provider<ICsvInventoryImporter> inventoryImporterProvider;
	private final Provider<ICsvOrderImporter> orderImporterProvider;

	//TODO hacked here to prevent multiple executions
	private static final ExecutorService purgeExecutor = Executors.newSingleThreadExecutor();
	private static TenantCallable lastExecutionTask; 
	
	@Setter
	private Facility facility;

	@Context
	private ResourceContext resourceContext;

	@Inject
	public FacilityResource(WorkBehavior workService,
		OrderBehavior orderService,
		NotificationBehavior notificationService,
		WebSocketManagerService webSocketManagerService,
		UiUpdateBehavior uiUpdateService,
		PropertyService propertyService,
		Provider<ICsvAislesFileImporter> aislesImporterProvider,
		Provider<ICsvLocationAliasImporter> locationsImporterProvider,
		Provider<ICsvInventoryImporter> inventoryImporterProvider,
		Provider<ICsvOrderImporter> orderImporterProvider) {
		this.orderService = orderService;
		this.workService = workService;
		this.webSocketManagerService = webSocketManagerService;
		this.notificationService = notificationService;
		this.uiUpdateService = uiUpdateService;
		this.propertyService = propertyService;
		this.aislesImporterProvider = aislesImporterProvider;
		this.locationsImporterProvider = locationsImporterProvider;
		this.inventoryImporterProvider = inventoryImporterProvider;
		this.orderImporterProvider = orderImporterProvider;
	}

	@Path("/import")
	public ImportResource getImportResource() throws Exception {
		ImportResource r = resourceContext.getResource(ImportResource.class);
	    r.setFacility(facility);
	    return r;
	}

	@Path("/edigateways")
	public EDIGatewaysResource getEdiResource() throws Exception {
		EDIGatewaysResource r = resourceContext.getResource(EDIGatewaysResource.class);
	    r.setFacility(facility);
	    return r;
	}

	@Path("/test")
	public TestResource getTestResource() throws Exception {
		TestResource r = resourceContext.getResource(TestResource.class);
	    r.setFacility(facility);
	    r.setTestBehavior(new TestBehavior());
	    return r;
	}

	
	@DELETE
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response detete(){
		try {
			facility.delete(webSocketManagerService);
			return BaseResponse.buildResponse("Facility Deleted");
		} catch (ConstraintViolationException e) {
			SQLException se = e.getSQLException();
			if (se != null){
				return new ErrorResponse().processException(se);
			} else {
				return new ErrorResponse().processException(e);
			}
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	
	@Path("/orders")
	public OrdersResource getOrders() {
		OrdersResource r = resourceContext.getResource(OrdersResource.class);
	    r.setFacility(facility);
	    return r;
	}


	@Path("/extensionpoints")
	@Produces(MediaType.APPLICATION_JSON)
	public ExtensionPointsResource getExtensionPoints() {
		ExtensionPointsResource r = resourceContext.getResource(ExtensionPointsResource.class);
		r.setFacility(facility);
		return r;
	}
	
	@GET
	@Path("/healthchecks/{type}/configuration")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getHealthCheckConfig(@PathParam("type") String healthCheckType) throws ScriptException {
		if ("DataQuantity".equalsIgnoreCase(healthCheckType)) {
			ExtensionPointService epService = ExtensionPointService.createInstance(facility);
			Optional<ExtensionPoint> extensionPoint = epService.getDataQuantityHealthCheckExtensionPoint();
			ParameterSetBeanABC parameterSet = 	epService.getDataQuantityHealthCheckParameters();
		    Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("parameterSet", parameterSet);
			responseMap.put("extensionPoint", extensionPoint.orNull());
			return BaseResponse.buildResponse(responseMap);
		} else {
			ErrorResponse error = new ErrorResponse();
			error.addBadParameter("type", healthCheckType);
			return BaseResponse.buildResponse(error, Response.Status.BAD_REQUEST);
		}
	}
	
	
	@GET
	@Path("/data/summary")
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDataSummary(@QueryParam("daysOld") int daysOld) {
		List<String> summary = workService.reportAchiveables(daysOld, this.facility);
		return BaseResponse.buildResponse(summary);
	}

	@DELETE
	@Path("/data/purge")
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteOldObjects(final @FormParam("daysOld") int daysOld) {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		UserContext userContext = CodeshelfSecurityManager.getCurrentUserContext();
		TenantCallable purgeCallable = new TenantCallable(persistenceService, tenant, userContext, new BatchProcessor() {

			private boolean	done = false;
			@Override
			public int doSetup() {
				return 1;
			}

			@Override
			public int doBatch(int batchCount) {
				//TODO do smaller batches of each
				Facility reloadedFacility = FacilityResource.this.facility.reload();
				workService.purgeOldObjects(daysOld, reloadedFacility, OrderHeader.class);
				workService.purgeOldObjects(daysOld, reloadedFacility, WorkInstruction.class);
				workService.purgeOldObjects(daysOld, reloadedFacility, Container.class);
				deleteWorkInstructions(daysOld);
				deleteContainers(daysOld);
				LOGGER.info("Async Purge Task Complete");
				done  = true;
				return 1;
			}

			@Override
			public int doTeardown() {
				return 1;
			}

			@Override
			public boolean isDone() {
				return done;
			}
			
			@Override
			public String toString() {
				return "PurgeAllAtOnceProcessor daysOld " + daysOld;
			}
		});
		//TODO do better prevention
		if (lastExecutionTask != null && lastExecutionTask.isRunning()) {
			LOGGER.info("Cancelling data purge task {}", lastExecutionTask);
				ListenableFuture<Void> cancelLast = lastExecutionTask.cancel();
				try {
					cancelLast.get(2, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					LOGGER.warn("Last purge task cancellation did not complete after 2 seconds: {}", lastExecutionTask, e);
				}
		}
		lastExecutionTask = purgeCallable;
		LOGGER.info("Submitted data purge task {}", purgeCallable);
		purgeExecutor.submit(purgeCallable);
		return BaseResponse.buildResponse(null);
	}

	
	@DELETE
	@Path("/data/wis")
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteWorkInstructions(@FormParam("daysOld") int daysOld) {
		workService.purgeOldObjects(daysOld, this.facility, WorkInstruction.class);
		return BaseResponse.buildResponse(null);
	}

	@DELETE
	@Path("/data/orders")
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteOrders(@FormParam("daysOld") int daysOld) {
		workService.purgeOldObjects(daysOld, this.facility, OrderHeader.class);
		return BaseResponse.buildResponse(null);
	}

	@DELETE
	@Path("/data/containers")
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteContainers(@FormParam("daysOld") int daysOld) {
		workService.purgeOldObjects(daysOld, this.facility, Container.class);
		return BaseResponse.buildResponse(null);
	}
	
	@GET
	@Path("/work/instructions/references")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findWorkInstructionReferences(@QueryParam("itemId") String itemIdSubstring, @QueryParam("containerId") String containerIdSubstring, @QueryParam("assigned") String assigneddDateSpec) {
		
		Interval assigneddInterval = null;
		if (assigneddDateSpec != null) {
			assigneddInterval = Interval.parse(assigneddDateSpec);
		}
		List<Object[]> results = this.workService.findWorkInstructionReferences(facility, assigneddInterval, itemIdSubstring, containerIdSubstring);
		return BaseResponse.buildResponse(results);
	
	}
	
	@GET
	@Path("/work/instructions/{persistentId}")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkInstruction(@PathParam("persistentId") UUIDParam persistentId, @QueryParam("properties") List<String> propertyNamesList) {
    	String[] propertyNames = new String[] {

    			 "groupAndSortCode",
    			 "pickerId",
    			 "type",
    			 "assigned",
    			 "completed",
    			 "pickInstructionUi",
    			 "nominalLocationId",
    			 "wiPosAlongPath",
    			 "description",
    			 "itemMasterId",
    			 "planQuantity",
    			 "uomMasterId",
    			 "uomNormalized",
    			 "orderId",
    			 "orderDetailId",
    			 "containerId",
    			 "assignedCheName",
    			 "domainId",
    			 "persistentId",
    			 "status",
    			 "planMinQuantity",
    			 "planMaxQuantity",
    			 "actualQuantity",
    			 "litLedsForWi",
    			 "gtin",
    			 "needsScan"
    	};

		List<Map<String, Object>> results = this.workService.findtWorkInstructions(facility, propertyNames, persistentId.getValue());
		if (results.size() == 1) {
			return BaseResponse.buildResponse(results.get(0));
		} else if (results.size() == 0){
			return BaseResponse.buildResponse(null);
			
		} else {
			LOGGER.error("Found multiple orders for {} in facility {}", persistentId, facility); 
			return BaseResponse.buildResponse(null);
		}
	}

    @GET
	@Path("/work/results")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkResults(@QueryParam("startTimestamp") TimestampParam startTimestamp, @QueryParam("endTimestamp") TimestampParam endTimestamp) {
    	List<WorkInstruction> results = this.workService.getWorkResults(facility.getPersistentId(), startTimestamp.getValue(), endTimestamp.getValue());
		return BaseResponse.buildResponse(results);
	}


	@GET
	@Path("/work/topitems")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkByItem() {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		return BaseResponse.buildResponse(this.orderService.itemsInQuantityOrder(session, facility.getPersistentId()));
	}

	/*
	@GET
	@Path("/groupinstructions")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGroupInstructions(@QueryParam("group") String groupName) {
		List<WorkInstruction> instructions = orderService.getGroupShortInstructions(facility.getPersistentId(), groupName);
		return BaseResponse.buildResponse(instructions);
	}*/

	@GET
	@Path("filters")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFilterNames() {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		Set<String> filterNames = orderService.getFilterNames(session);
		return BaseResponse.buildResponse(filterNames);
	}

	@GET
	@Path("/statussummary/{aggregate}")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrderStatusSummary(@PathParam("aggregate") String aggregate, @QueryParam("filterName") String filterName) {
		@SuppressWarnings("unused")
		ErrorResponse errors = new ErrorResponse();
		if (Strings.isNullOrEmpty(filterName)) {
			//errors.addParameterError("filterName", ErrorCode.FIELD_REQUIRED);
		}
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		ProductivitySummaryList.StatusSummary summary = orderService.statusSummary(session, facility.getPersistentId(), aggregate, filterName);

		return BaseResponse.buildResponse(summary);
	}

	@GET
	@Path("/ches")
	@RequiresPermissions("che:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllChesInFacility() {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", facility));
		Criteria cheCriteria = Che.staticGetDao().createCriteria();
		cheCriteria.createCriteria("parent", "network").add(Restrictions.eq("parent", facility));
		List<Che> ches = Che.staticGetDao().findByCriteriaQuery(cheCriteria);
		return BaseResponse.buildResponse(ches);
	}


	@GET
	@Path("/workers")
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllWorkersInFacility() {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", facility));
		List<Worker> workers = Worker.staticGetDao().findByFilter(filterParams);
		return BaseResponse.buildResponse(workers);
	}

	@POST
	@Path("/workers")
	@RequiresPermissions("worker:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createWorker(Worker worker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			worker.setFacility(facility);
			worker.generateDomainId();
			if (worker.getActive() == null){
				worker.setActive(true);
			}
			worker.setUpdated(new Timestamp(System.currentTimeMillis()));
			if (!worker.isValid(errors)){
				errors.setStatus(Status.BAD_REQUEST);
				return errors.buildResponse();
			}
			UUID id = worker.getPersistentId();
			Worker.staticGetDao().store(worker);
			Worker createdWorker = Worker.staticGetDao().findByPersistentId(id);
			return BaseResponse.buildResponse(createdWorker);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@GET
	@Path("events")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchEvents(
		@QueryParam("type") List<EventTypeParam> typeParamList,
		@QueryParam("itemId") String itemId,
		@QueryParam("location") String location,
		@QueryParam("workerId") String workerId,
		@QueryParam("groupBy") String groupBy,
		@QueryParam("resolved") Boolean resolved ) {
		ErrorResponse errors = new ErrorResponse();
		try {
			List<Criterion> filterParams = new ArrayList<Criterion>();
			filterParams.add(Restrictions.eq("facility", facility));
			//If any "type" parameters are provided, filter accordingly
			List<WorkerEvent.EventType> typeList = Lists.newArrayList();
			for (EventTypeParam type : typeParamList) {
				if (type != null) {
					typeList.add(type.getValue());
				}
			}
			if (!typeList.isEmpty()) {
				filterParams.add(Restrictions.in("eventType", typeList));
			}

			if (!Strings.isNullOrEmpty(itemId)) {
				//do by filter param but for now needs to be manually filtered
			}

			//If "resolved" parameter not provided, return, both, resolved and unresolved events
			if (resolved != null) {
				if (resolved){
					filterParams.add(Restrictions.isNotNull("resolution"));
				} else {
					filterParams.add(Restrictions.isNull("resolution"));
				}
			}

			
			
			if (!Strings.isNullOrEmpty(itemId)) {
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				ResultDisplay result = new ResultDisplay();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					ItemDisplay itemDisplayKey = new ItemDisplay(eventDisplay);
					if (itemId.equals(itemDisplayKey.getItemId()) &&
						location.equals(itemDisplayKey.getLocation())) {
						result.add(new BeanMap(eventDisplay));
					}
				}
				return BaseResponse.buildResponse(result);
			} else if (!Strings.isNullOrEmpty(workerId)) {
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				ResultDisplay result = new ResultDisplay();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					WorkerDisplay workerDisplayKey = new WorkerDisplay(eventDisplay);
					if (workerId.equals(workerDisplayKey.getId())) {
						result.add(new BeanMap(eventDisplay));
					}
				}
				return BaseResponse.buildResponse(result);
			}



			if ("item".equals(groupBy)) {
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				Map<ItemDisplay, Integer> issuesByItem = new HashMap<>();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					ItemDisplay itemDisplayKey = new ItemDisplay(eventDisplay);
					Integer count = MoreObjects.firstNonNull(issuesByItem.get(itemDisplayKey), 0);
					issuesByItem.put(itemDisplayKey, count+1);
				}

				ResultDisplay result = new ResultDisplay(ItemDisplay.ItemComparator);
				for (Map.Entry<ItemDisplay, Integer> issuesByItemEntry : issuesByItem.entrySet()) {
					Map<Object, Object> values = new HashMap<>();
					values.putAll(new BeanMap(issuesByItemEntry.getKey()));
					values.put("count", issuesByItemEntry.getValue());
					result.add(values);
				}
				return BaseResponse.buildResponse(result);
			} else if ("worker".equals(groupBy)) {	
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				Map<WorkerDisplay, Integer> issuesByWorker = new HashMap<>();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					WorkerDisplay workerDisplayKey = new WorkerDisplay(eventDisplay);
					Integer count = MoreObjects.firstNonNull(issuesByWorker.get(workerDisplayKey), 0);
					issuesByWorker.put(workerDisplayKey, count+1);
				}

				ResultDisplay result = new ResultDisplay(WorkerDisplay.ItemComparator);
				for (Map.Entry<WorkerDisplay, Integer> issuesByWorkerEntry : issuesByWorker.entrySet()) {
					Map<Object, Object> values = new HashMap<>();
					values.putAll(new BeanMap(issuesByWorkerEntry.getKey()));
					values.put("count", issuesByWorkerEntry.getValue());
					result.add(values);
				}
				return BaseResponse.buildResponse(result);
			} else if ("type".equals(groupBy)){
				List<WorkerEventTypeGroup> issuesByType = notificationService.groupWorkerEventsByType(facility, resolved);
				ResultDisplay result = new ResultDisplay(issuesByType.size());
		        result.addAll(issuesByType);	
				return BaseResponse.buildResponse(result);
			} else {
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				ResultDisplay result = new ResultDisplay(events.size());
				for (WorkerEvent event : events) {
					result.add(new BeanMap(EventDisplay.createEventDisplay(event)));
				}
				return BaseResponse.buildResponse(result);
			}


		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		}
	}

	@GET
	@Path("pickrate")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pickRate(@QueryParam("startTimestamp") TimestampParam startDateParam, @QueryParam("endTimestamp") TimestampParam endDateParam) {
		ErrorResponse errors = new ErrorResponse();
		try {
			List<PickRate> pickRates = notificationService.getPickRate(new DateTime(startDateParam.getValue()), new DateTime(endDateParam.getValue()));
			return BaseResponse.buildResponse(pickRates);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@POST
	@Path("/process_script")
	@RequiresPermissions("che:simulate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response runScriptSteps(FormDataMultiPart body){
		try {
			ErrorResponse errors = new ErrorResponse();

			//Retrieve the script
			InputStream scriptIS = ScriptStepParser.getInputStream(body, "script");
			if (scriptIS == null) {
				errors.addErrorMissingBodyParam("script");
				return errors.buildResponse();
			}
			String script = IOUtils.toString(scriptIS);
			if (script == null || script.isEmpty()) {
				errors.addError("Script file was empty");
				return errors.buildResponse();
			}
			ScriptStep firstStep = ScriptParser.parseScript(script);
			firstStep.setReport("Script imported");
			//return BaseResponse.buildResponse(new ScriptApiResponse(firstStep, "Script imported"));
			return BaseResponse.buildResponse(firstStep);
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	@POST
	@Path("/run_script")
	@RequiresPermissions("che:simulate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response runScript(@QueryParam("script_step_id") UUIDParam scriptStepId,
		@QueryParam("timeout_min") Integer timeoutMin,
		FormDataMultiPart body){
		TenantPersistenceService persistence = TenantPersistenceService.getInstance();
		try {
			ErrorResponse errors = new ErrorResponse();
			
			//Verify that this Script Server Runner was called with an active transaction, and close it.
			//The Server and Site script runners below will manage transactions themselves
			if (!persistence.hasAnyActiveTransactions()) {
				errors.addError("Server Error: FacilityResponse.runScript() called without an active transaction");
				return errors.buildResponse();
			}			
			UUID facilityId = facility.getPersistentId();
			persistence.commitTransaction();

			if (!BaseResponse.isUUIDValid(scriptStepId, "script_step_id", errors)){
				return errors.buildResponse();
			}

			ScriptStep scriptStep = ScriptParser.getScriptStep(scriptStepId.getValue());
			if (scriptStep == null) {
				errors.addError("Script step " + scriptStepId.getValue() + " doesn't exist");
				return errors.buildResponse();
			}
			//Split the script into a list of SERVER and SITE parts
			ArrayList<StepPart> scriptParts = scriptStep.parts();
			StringBuilder report = new StringBuilder("Running script step " + scriptStep.getComment() + "\n");

			ScriptServerRunner serverScriptRunner = new ScriptServerRunner(facilityId,
				body,
				uiUpdateService,
				propertyService,
				aislesImporterProvider,
				locationsImporterProvider,
				inventoryImporterProvider,
				orderImporterProvider);
			String error = null;
			//Process script parts
			while (!scriptParts.isEmpty()) {
				StepPart part = scriptParts.remove(0);
				ScriptMessage responseMessage = null;
				if (part.isServer()) {
					//SERVER
					responseMessage = serverScriptRunner.processServerScript(part.getScriptLines());
				} else {
					//SITE
					responseMessage = runSiteScript(part, timeoutMin);
				}
				report.append(responseMessage.getResponse());
				if (!responseMessage.isSuccess()) {
					error = responseMessage.getError();
					break;
				}
			}

			if (error != null) {
				ScriptStep nextStepError = new ScriptStep(report.toString());
				nextStepError.addError(error);
				return BaseResponse.buildResponse(nextStepError, Status.BAD_REQUEST);
			}
			
			ScriptStep nextStep = scriptStep.nextStep();
			if (nextStep == null) {
				return BaseResponse.buildResponse(new ScriptStep(report.toString()));
			} else {
				nextStep.setReport(report.toString());
				return BaseResponse.buildResponse(nextStep);
			}
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		} finally {
			//Open a new transaction for TransactionFilter to close later
			if (!persistence.hasAnyActiveTransactions()) {
				persistence.beginTransaction();
			}
		}
	}
	
	private ScriptMessage runSiteScript(StepPart part, int timeoutMin){
		ScriptMessage errorMesage = new ScriptMessage();
		TenantPersistenceService persistence = TenantPersistenceService.getInstance();
		//Test is Site Controller is running
		Result siteHealth = new ActiveSiteControllerHealthCheck(webSocketManagerService).execute();
		if (!siteHealth.isHealthy()){ 
			errorMesage.setMessageError("Site controller problem: " + siteHealth.getMessage());
			return errorMesage;
		}
		persistence.beginTransaction();
		try {
			facility = Facility.staticGetDao().reload(facility);
			Set<User> users = facility.getSiteControllerUsers();
			if (users == null || users.isEmpty()) {
				errorMesage.setMessageError("Could not communicate with site controller. Check Site Controller ID.");
				return errorMesage;
			}
			//Execute script
			UUID id = UUID.randomUUID();
			ScriptMessage scriptMessage = new ScriptMessage(id, part.getScriptLines());
			webSocketManagerService.sendMessage(users, scriptMessage);
			persistence.commitTransaction();
			ScriptMessage siteResponseMessage = ScriptSiteCallPool.waitForSiteResponse(id, timeoutMin);
			if (siteResponseMessage == null) {
				errorMesage.setMessageError("Site request timed out");
				return errorMesage; 
			}
			return siteResponseMessage;
		} catch (Exception e) {
			persistence.rollbackTransaction();
			errorMesage.setMessageError("Site request failed: " + e.getMessage());
			return errorMesage; 
		}
	}

	@PUT
	@Path("hardware")
	@RequiresPermissions("companion:view")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response performHardwareAction(HardwareRequest req) {
		ErrorResponse errors = new ErrorResponse();
		if (!req.isValid(errors)){
			return errors.buildResponse();
		}
		try {
			Set<User> users = facility.getSiteControllerUsers();

			//LIGHTS
			List<LedSample> ledSamples = new ArrayList<LedSample>();

			if (req.getLights() != null) {
				for (LightRequest light :req.getLights()){
					ledSamples.add(new LedSample(light.getPosition(), light.getColor()));
				}

				LedCmdGroup ledCmdGroup = new LedCmdGroup(req.getLightController(), req.getLightChannel(), (short)0, ledSamples);
				LightLedsInstruction instruction = new LightLedsInstruction(req.getLightController(), req.getLightChannel(), req.getLightDuration(), ImmutableList.of(ledCmdGroup));
				LedInstrListMessage lightMessage = new LedInstrListMessage(instruction);
				webSocketManagerService.sendMessage(users, lightMessage);
			}

			//CHE MESSAGES
			if (req.getCheMessages() != null) {
				for (CheDisplayRequest cheReq : req.getCheMessages()) {
					CheDisplayMessage cheMessage = new CheDisplayMessage(cheReq.getChe(), cheReq.getLine1(), cheReq.getLine2(), cheReq.getLine3(), cheReq.getLine4());
					webSocketManagerService.sendMessage(users, cheMessage);
				}
			}

			//POSCON MESSAGES
			if (req.getPosConInstructions() != null) {
				for (PosControllerInstr posInstr : req.getPosConInstructions()) {
					posInstr.prepareObject();
					Thread.sleep(1000);
					webSocketManagerService.sendMessage(users, posInstr);
				}
			}

			return BaseResponse.buildResponse("Commands Sent");
		} catch (Exception e) {
			return errors.processException(e);
		}
	}
	
	
}
