package com.codeshelf.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.EdiExporterProvider;
import com.codeshelf.edi.FacilityEdiExporter;
import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WiSetSummary;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.request.ServiceMethodRequest;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ServiceMethodResponse;
import com.codeshelf.ws.server.ServerMessageProcessor;
import com.google.common.collect.ImmutableList;

public class WorkServiceTest extends ServerTest {
	private static final Logger			LOGGER		= LoggerFactory.getLogger(WorkServiceTest.class);

	private WorkInstructionGenerator	wiGenerator	= new WorkInstructionGenerator();
	private FacilityGenerator			facilityGenerator;


	@Override
	public void doBefore() {
		super.doBefore();
		facilityGenerator = new FacilityGenerator();
	}

	@Test
	public void workResultActualOnly() {
		//create order detail
		//computeWI
		//getWI
		//complete
		//query
	}

	/*
	@Test
	public void workLocationConflictPreferredDoesNotExist() {
		this.workService = new WorkService();
		this.initializeEphemeralServiceManager();

		Facility facility = Mockito.mock(Facility.class);//facilityModels.setupSimpleNotSlottedFacility();
	//		Facility facility = mock(facility)
		Che che = Mockito.mock(Che.class);
		when(che.getFacility()).thenReturn(facility);
		
		ItemMaster itemMaster = mock(ItemMaster.class);
		
		String itemLocation = "Loc1";
		Item item = mock(Item.class); 
		when(item.getItemLocationAlias()).thenReturn(itemLocation);
		when(item.getParent()).thenReturn(itemMaster);
		

		String preferredLocation = "LocX";
		
		OrderDetail orderDetail = mock(OrderDetail.class);
		when(orderDetail.getPreferredLocation()).thenReturn(preferredLocation);
		when(orderDetail.getItemMaster()).thenReturn(itemMaster);
		
		String containerId = "CONT1";
		
		//this.getTenantPersistenceService().beginTransaction();// TEMP
		WorkList workList = this.workService.computeWorkInstructions(che, ImmutableList.of(containerId));
		Assert.assertEquals(preferredLocation, workList.getInstructions().get(0).getLocationId());

		//this.getTenantPersistenceService().commitTransaction();// TEMP
	}*/
	/*
		@Test
		public void workLocationConflictPreferredNoItem() {
			Facility facility = facilityModels.setupSimpleNotSlottedFacility();
			Che che = facilityModels.getAnyChe(facility);

			String itemLocation = "Loc1";
			Assert.assertNotNull(facility.findLocation(itemLocation));
			Item item = createItem("Loc1"); 

			String preferredLocation = "Loc2";
			Assert.assertNotNull(facility.findLocation(preferredLocation));
			
			Assert.assertNull(facility.findItem(preferredLocation));
			
			OrderDetail orderDetail = createOrderDetail(item, preferredLocation);
			
			WorkList workList = workService.computeWorkInstructions(che, ImmutableList.of(orderDetail.getParent().getContainerId()));
			Assert.assertEquals(preferredLocation, workList.getInstructions().get(0).getLocationId());
		}*/

	@Test
	public void shortedWorkInstructionShortsOrderDetail() {
		this.workService = new WorkService(mock(LightService.class), mock(EdiExporterProvider.class));

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wiToRecord = generateValidWorkInstruction(facility, new Timestamp(0));
		UUID detailId = wiToRecord.getOrderDetail().getPersistentId();
		UUID cheId = wiToRecord.getAssignedChe().getPersistentId();
		this.getTenantPersistenceService().commitTransaction();

		wiToRecord.setStatus(WorkInstructionStatusEnum.SHORT);

		this.getTenantPersistenceService().beginTransaction();
		OrderDetail priorOrderDetail = OrderDetail.staticGetDao().findByPersistentId(detailId);
		Assert.assertNotEquals(OrderStatusEnum.SHORT, priorOrderDetail.getStatus());

		workService.completeWorkInstruction(cheId, wiToRecord);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		OrderDetail updatedOrderDetail = OrderDetail.staticGetDao().findByPersistentId(detailId);
		Assert.assertEquals(OrderStatusEnum.SHORT, updatedOrderDetail.getStatus());
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void workSummaryRequest() {
		this.getTenantPersistenceService().beginTransaction();

		LOGGER.info("1: get the facility");
		Facility facility = facilityGenerator.generateValid();
		UUID cheId = firstChe(facility).getPersistentId();

		LOGGER.info("1: mock request, work server, and service factory");
		ServiceMethodRequest request = new ServiceMethodRequest();
		request.setClassName("WorkService"); //the ux would use strings
		request.setMethodName("workAssignedSummary");
		request.setMethodArgs(ImmutableList.of(cheId.toString(), facility.getPersistentId().toString()));
		WorkService workService = mock(WorkService.class);
		when(workService.workAssignedSummary(eq(cheId), eq(facility.getPersistentId()))).thenReturn(Collections.<WiSetSummary> emptyList());
		ServiceFactory factory = new ServiceFactory(workService,
			mock(LightService.class),
			mock(DummyPropertyService.class),
			mock(UiUpdateService.class),
			mock(OrderService.class),
			mock(InventoryService.class),
			mock(NotificationService.class),
			mock(InfoService.class),
			mock(PalletizerService.class));
		this.getTenantPersistenceService().commitTransaction();

		IMessageProcessor processor = new ServerMessageProcessor(factory,
			new ConverterProvider().get(),
			this.webSocketManagerService);
		ResponseABC responseABC = processor.handleRequest(this.getMockWsConnection(), request);
		Assert.assertTrue(responseABC instanceof ServiceMethodResponse);
		Assert.assertTrue(responseABC.isSuccess());

		ServiceMethodRequest request2 = new ServiceMethodRequest();
		request2.setClassName("WorkService"); //the ux would use strings
		request2.setMethodName("workCompletedSummary");
		request2.setMethodArgs(ImmutableList.of(cheId.toString(), facility.getPersistentId().toString()));
		WorkService workService2 = mock(WorkService.class);
		when(workService2.workCompletedSummary(eq(cheId), eq(facility.getPersistentId()))).thenReturn(Collections.<WiSetSummary> emptyList());
		ServiceFactory factory2 = new ServiceFactory(workService2,
			mock(LightService.class),
			mock(DummyPropertyService.class),
			mock(UiUpdateService.class),
			mock(OrderService.class),
			mock(InventoryService.class),
			mock(NotificationService.class),
			mock(InfoService.class),
			mock(PalletizerService.class));
		IMessageProcessor processor2 = new ServerMessageProcessor(factory2,
			new ConverterProvider().get(),
			this.webSocketManagerService);
		ResponseABC responseABC2 = processor2.handleRequest(this.getMockWsConnection(), request2);
		Assert.assertTrue(responseABC2 instanceof ServiceMethodResponse);
		Assert.assertTrue(responseABC2.isSuccess());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void summariesAreSorted() {
		this.workService = new WorkService(mock(LightService.class), mock(EdiExporterProvider.class));

		this.getTenantPersistenceService().beginTransaction();

		Facility facility = facilityGenerator.generateValid();

		ITypedDao<WorkInstruction> workInstructionDao = mock(ITypedDao.class);
		this.useCustomDao(WorkInstruction.class, workInstructionDao);

		ArrayList<WorkInstruction> inputs = new ArrayList<WorkInstruction>();
		for (int i = 0; i < 4; i++) {
			inputs.add(generateValidWorkInstruction(facility, nextUniquePastTimestamp()));
		}
		when(workInstructionDao.findByFilter(anyList())).thenReturn(inputs);

		Che che = firstChe(facility);
		List<WiSetSummary> workSummaries = workService.workAssignedSummary(che.getPersistentId(), facility.getPersistentId());

		//since each timestamp is unique they will each get summarized into their own summary object
		Assert.assertEquals(inputs.size(), workSummaries.size());
		Timestamp lastTimestamp = new Timestamp(Long.MAX_VALUE);
		for (WiSetSummary wiSetSummary : workSummaries) {
			Timestamp thisTime = wiSetSummary.getAssignedTime();
			Assert.assertTrue(thisTime.toString() + "should have been before" + lastTimestamp, wiSetSummary.getAssignedTime()
				.before(lastTimestamp));
			lastTimestamp = wiSetSummary.getAssignedTime();
		}

		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void completeWorkInstructionExceptionIfNotFound() throws Exception {

		this.getTenantPersistenceService().beginTransaction();

		UUID cheId = UUID.randomUUID();

		createWorkService(Integer.MAX_VALUE, mock(FacilityEdiExporter.class), 1L);
		WorkInstruction wiToRecord = generateValidWorkInstruction(facilityGenerator.generateValid(), new Timestamp(0));

		this.<Che> useCustomDao(Che.class, mock(ITypedDao.class));
		when(Che.staticGetDao().findByPersistentId(eq(cheId))).thenReturn(new Che());

		this.<WorkInstruction> useCustomDao(WorkInstruction.class, mock(ITypedDao.class));
		when(WorkInstruction.staticGetDao().findByPersistentId(eq(wiToRecord.getPersistentId()))).thenReturn(null);

		try {
			workService.completeWorkInstruction(cheId, wiToRecord);
			Assert.fail("recordCompletedWorkInstruction should have thrown an exception if WI cannot be found");
		} catch (InputValidationException e) {
			Assert.assertNotNull(e.getErrors().getFieldErrors("persistentId"));
			Assert.assertFalse(e.getErrors().getFieldErrors("persistentId").isEmpty());
		}
		verify(WorkInstruction.staticGetDao(), never()).store(any(WorkInstruction.class));

		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void doesNotExportIfWICannotBeStored() throws Exception {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction existingWi = generateValidWorkInstruction(facility, new Timestamp(0));
		WorkInstruction wiToRecord = generateValidWorkInstruction(facility, new Timestamp(0));
		this.getTenantPersistenceService().commitTransaction();

		FacilityEdiExporter mockEdiExportService = mock(FacilityEdiExporter.class);
		createWorkService(Integer.MAX_VALUE, mockEdiExportService, 1L);

		UUID cheId = UUID.randomUUID();
		this.<Che> useCustomDao(Che.class, mock(ITypedDao.class));
		when(Che.staticGetDao().findByPersistentId(eq(cheId))).thenReturn(new Che());

		UUID testId = UUID.randomUUID();

		existingWi.setPersistentId(testId);
		wiToRecord.setPersistentId(testId);

		ITypedDao<WorkInstruction> wiDao = mock(ITypedDao.class);
		useCustomDao(WorkInstruction.class, wiDao);
		this.<OrderDetail> useCustomDao(OrderDetail.class, mock(ITypedDao.class));
		this.<OrderHeader> useCustomDao(OrderHeader.class, mock(ITypedDao.class));
		when(wiDao.findByPersistentId(eq(wiToRecord.getPersistentId()))).thenReturn(existingWi);

		doThrow(new DaoException("test")).when(WorkInstruction.staticGetDao()).store(eq(wiToRecord));

		workService.completeWorkInstruction(cheId, wiToRecord);

		verify(mockEdiExportService, never()).exportWiFinished(any(OrderHeader.class),  any(Che.class), any(WorkInstruction.class));
	}

	
	@Test
	public void allWorkInstructionsSent() throws Exception {
		this.getTenantPersistenceService().beginTransaction();

		FacilityEdiExporter mockEdiExportService = mock(FacilityEdiExporter.class);
		int total = 100;
		createWorkService(total + 1, mockEdiExportService, 1L);
		List<WorkInstruction> wiList = generateValidWorkInstructions(total);
		for (WorkInstruction wi : wiList) {
			workService.completeWorkInstruction(wi.getAssignedChe().getPersistentId(), wi);

		}

		verify(mockEdiExportService, Mockito.timeout(2000).times(total)).exportWiFinished(any(OrderHeader.class),  any(Che.class), any(WorkInstruction.class));

		this.getTenantPersistenceService().commitTransaction();
	}


	private void createWorkService(int capacity, FacilityEdiExporter ediExporter, long retryDelay) throws Exception {

		EdiExporterProvider provider = mock(EdiExporterProvider.class);
		when(provider.getEdiExporter(any(Facility.class))).thenReturn(ediExporter);

		this.workService = new WorkService(mock(LightService.class), provider);
	}

	private List<WorkInstruction> generateValidWorkInstructions(int total) {
		ArrayList<WorkInstruction> wiList = new ArrayList<WorkInstruction>();
		Facility facility = facilityGenerator.generateValid();
		for (int i = 0; i < total; i++) {
			WorkInstruction wi = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
			wiList.add(wi);
		}
		return wiList;
	}

	private WorkInstruction generateValidWorkInstruction(Facility facility, Timestamp timestamp) {
		WorkInstruction wi = wiGenerator.generateValid(facility);
		wi.setAssigned(timestamp);
		return wi;
	}

	private Timestamp nextUniquePastTimestamp() {
		return new Timestamp(System.currentTimeMillis() - Math.abs(RandomUtils.nextLong()));
	}

	private Che firstChe(Facility facility) {
		Che firstChe = null;
		for (CodeshelfNetwork network : facility.getNetworks()) {
			for (Che che : network.getChes().values()) {
				firstChe = che;
				break;
			}
		}
		return firstChe;
	}

}
