package com.codeshelf.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.invocation.InvocationOnMock;

import com.codeshelf.edi.IEdiExportServiceProvider;
import com.codeshelf.edi.WorkInstructionCSVExporter;
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
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.protocol.request.ServiceMethodRequest;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ServiceMethodResponse;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.WebSocketConnection;
import com.google.common.collect.ImmutableList;

public class WorkServiceTest extends ServerTest {
	
	private WorkInstructionGenerator wiGenerator = new WorkInstructionGenerator();
	private FacilityGenerator facilityGenerator;
	
	@Override
	public void doBefore() {
		super.doBefore();
		facilityGenerator = new FacilityGenerator(getDefaultTenant());
	}

	@Override
	protected WorkService generateWorkService() {
		return this.workService; // in this test, we set up the workService manually at the beginning of the test
	}

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false; // in this test, we start services manually after defining the work service to start
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
		this.workService = new WorkService();
		this.initializeEphemeralServiceManager();
		
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

		workService.completeWorkInstruction(cheId,	wiToRecord);
		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();
		OrderDetail updatedOrderDetail = OrderDetail.staticGetDao().findByPersistentId(detailId);
		Assert.assertEquals(OrderStatusEnum.SHORT, updatedOrderDetail.getStatus());
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void workSummaryRequest() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = facilityGenerator.generateValid();
		UUID cheId = firstChe(facility);
		
		ServiceMethodRequest request = new ServiceMethodRequest();
		request.setClassName("WorkService"); //the ux would use strings
		request.setMethodName("workAssignedSummary");
		request.setMethodArgs(ImmutableList.of(cheId.toString(), facility.getPersistentId().toString()));
		WorkService workService = mock(WorkService.class);
		when(workService.workAssignedSummary(eq(cheId), eq(facility.getPersistentId()))).thenReturn(Collections.<WiSetSummary>emptyList());
		ServiceFactory factory = new ServiceFactory(workService, mock(LightService.class), mock(DummyPropertyService.class), mock(UiUpdateService.class), mock(OrderService.class), mock(InventoryService.class));
		IMessageProcessor processor = new ServerMessageProcessor(factory, new ConverterProvider().get(), this.webSocketManagerService);
		ResponseABC responseABC = processor.handleRequest(mock(WebSocketConnection.class), request);
		Assert.assertTrue(responseABC instanceof ServiceMethodResponse);
		Assert.assertTrue(responseABC.isSuccess());

		ServiceMethodRequest request2 = new ServiceMethodRequest();
		request2.setClassName("WorkService"); //the ux would use strings
		request2.setMethodName("workCompletedSummary");
		request2.setMethodArgs(ImmutableList.of(cheId.toString(), facility.getPersistentId().toString()));
		WorkService workService2 = mock(WorkService.class);
		when(workService2.workCompletedSummary(eq(cheId), eq(facility.getPersistentId()))).thenReturn(Collections.<WiSetSummary>emptyList());
		ServiceFactory factory2 = new ServiceFactory(workService2, mock(LightService.class), mock(DummyPropertyService.class), mock(UiUpdateService.class), mock(OrderService.class), mock(InventoryService.class));
		IMessageProcessor processor2 = new ServerMessageProcessor(factory2, new ConverterProvider().get(), this.webSocketManagerService);
		ResponseABC responseABC2 = processor2.handleRequest(mock(WebSocketConnection.class), request2);
		Assert.assertTrue(responseABC2 instanceof ServiceMethodResponse);
		Assert.assertTrue(responseABC2.isSuccess());
		this.getTenantPersistenceService().commitTransaction();

	}
	
	

	@SuppressWarnings("unchecked")
	@Test
	public void summariesAreSorted() {
		this.workService = new WorkService();
		this.initializeEphemeralServiceManager();
		
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = facilityGenerator.generateValid();

		ITypedDao<WorkInstruction> workInstructionDao = mock(ITypedDao.class);
		this.useCustomDao(WorkInstruction.class, workInstructionDao);

		ArrayList<WorkInstruction> inputs = new ArrayList<WorkInstruction>();
		for (int i = 0; i < 4; i++) {
			inputs.add(generateValidWorkInstruction(facility, nextUniquePastTimestamp()));
		}
		when(workInstructionDao.findByFilter(anyList())).thenReturn(inputs);

		UUID cheId = firstChe(facility);
		List<WiSetSummary> workSummaries  = workService.workAssignedSummary(cheId, facility.getPersistentId());

		//since each timestamp is unique they will each get summarized into their own summary object
		Assert.assertEquals(inputs.size(), workSummaries.size());
		Timestamp lastTimestamp = new Timestamp(Long.MAX_VALUE);
		for (WiSetSummary wiSetSummary : workSummaries) {
			Timestamp thisTime = wiSetSummary.getAssignedTime();
			Assert.assertTrue(thisTime.toString() + "should have been before" + lastTimestamp, wiSetSummary.getAssignedTime().before(lastTimestamp));
			lastTimestamp = wiSetSummary.getAssignedTime();
		}

		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void completeWorkInstructionExceptionIfNotFound() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		UUID cheId = UUID.randomUUID();

		createWorkService(Integer.MAX_VALUE, mock(IEdiService.class), 1L);
		WorkInstruction wiToRecord = generateValidWorkInstruction(facilityGenerator.generateValid(), new Timestamp(0));

		this.<Che>useCustomDao(Che.class, mock(ITypedDao.class));
		when(Che.staticGetDao().findByPersistentId(eq(cheId))).thenReturn(new Che());

		this.<WorkInstruction>useCustomDao(WorkInstruction.class,mock(ITypedDao.class));
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
	public void doesNotExportIfWICannotBeStored() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction existingWi = generateValidWorkInstruction(facility, new Timestamp(0));
		WorkInstruction wiToRecord = generateValidWorkInstruction(facility, new Timestamp(0));
		this.getTenantPersistenceService().commitTransaction();

		IEdiService mockEdiExportService = mock(IEdiService.class);
		createWorkService(Integer.MAX_VALUE, mockEdiExportService, 1L);

		UUID cheId = UUID.randomUUID();
		this.<Che>useCustomDao(Che.class, mock(ITypedDao.class));
		when(Che.staticGetDao().findByPersistentId(eq(cheId))).thenReturn(new Che());

		UUID testId = UUID.randomUUID();

		existingWi.setPersistentId(testId);
		wiToRecord.setPersistentId(testId);

		ITypedDao<WorkInstruction> wiDao = mock(ITypedDao.class);
		useCustomDao(WorkInstruction.class,wiDao);
		this.<OrderDetail>useCustomDao(OrderDetail.class,mock(ITypedDao.class));
		this.<OrderHeader>useCustomDao(OrderHeader.class,mock(ITypedDao.class));
		when(wiDao.findByPersistentId(eq(wiToRecord.getPersistentId()))).thenReturn(existingWi);

		doThrow(new DaoException("test")).when(WorkInstruction.staticGetDao()).store(eq(wiToRecord));

		workService.completeWorkInstruction(cheId, wiToRecord);

		verify(mockEdiExportService, never()).sendWorkInstructionsToHost(any(String.class));
	}

	@Test
	public void allWorkInstructionsSent() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		IEdiService mockEdiExportService = mock(IEdiService.class);

		int total = 100;
		createWorkService(total+1, mockEdiExportService, 1L);
		List<WorkInstruction> wiList = generateValidWorkInstructions(total);
		for (WorkInstruction wi: wiList) {
			workService.exportWorkInstruction(wi);

		}

		verify(mockEdiExportService, Mockito.timeout(2000).times(total)).sendWorkInstructionsToHost(any(String.class));
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void workInstructionExportIsRetried() throws IOException, InterruptedException {
		this.getTenantPersistenceService().beginTransaction();

		long expectedRetryDelay = 1L;
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi = generateValidWorkInstruction(facility, nextUniquePastTimestamp());

		String messageBody = format(wi);
		
		IEdiService mockEdiExportService = mock(IEdiService.class);
		doThrow(new IOException("test io")).
		doThrow(new IOException("second one")).
		doNothing().when(mockEdiExportService).sendWorkInstructionsToHost(messageBody);

		createWorkService(Integer.MAX_VALUE, mockEdiExportService, expectedRetryDelay);
		workService.exportWorkInstruction(wi);

		//Wait up to a second per invocation to verify
		verify(mockEdiExportService, Mockito.timeout((int)(expectedRetryDelay * 5000L)).times(3)).sendWorkInstructionsToHost(eq(messageBody));
		
		this.getTenantPersistenceService().commitTransaction();
	}

	private String format(WorkInstruction wi) throws IOException {
		WorkInstructionCSVExporter exporter = new WorkInstructionCSVExporter();
		// TODO Auto-generated method stub
		return exporter.exportWorkInstructions(ImmutableList.of(wi));
	}

	@Test
	public void workInstructionExportRetriesAreDelayed() throws IOException, InterruptedException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
		String testMessage = format(wi);

		IEdiService mockEdiExportService = mock(IEdiService.class);

		List<Long> timings = new ArrayList<Long>();
		doAnswer(new TimedExceptionAnswer(new IOException("test io"), timings)).
		doAnswer(new TimedExceptionAnswer(new IOException("test io"), timings)).
		doAnswer(new TimedDoesNothing(timings)).
			when(mockEdiExportService).sendWorkInstructionsToHost(testMessage);

		long expectedRetryDelay = 2000;
		createWorkService(Integer.MAX_VALUE, mockEdiExportService, expectedRetryDelay);
		workService.exportWorkInstruction(wi);

		verify(mockEdiExportService, Mockito.timeout((int)(expectedRetryDelay * 1000L)).times(3)).sendWorkInstructionsToHost(eq(testMessage));
		long previousTimestamp = timings.remove(0);
		for (Long timestamp : timings) {
			long diff = timestamp - previousTimestamp;
			// change from diff > expectedRetryDelay to diff >= expectedRetryDelay. Not necessarily accurate to the ms, but JRs Mac caught this a lot.
			Assert.assertTrue("The delay between calls was not greater than " + expectedRetryDelay + "ms but was: " + diff, diff >= expectedRetryDelay);
		}

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void workInstructionContinuesOnRuntimeException() throws IOException, InterruptedException {
		this.getTenantPersistenceService().beginTransaction();

		long expectedRetryDelay = 1L;
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi1 = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
		WorkInstruction wi2 = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
		IEdiService mockEdiExportService = mock(IEdiService.class);
		doThrow(new RuntimeException("test io")).
		doNothing().when(mockEdiExportService).sendWorkInstructionsToHost(any(String.class));

		createWorkService(Integer.MAX_VALUE, mockEdiExportService, expectedRetryDelay);
		workService.exportWorkInstruction(wi1);
		workService.exportWorkInstruction(wi2);

		//Wait up to a second per invocation to verify
		// Normal behavior is to keep retrying the first on IOException
		//   in this case it should skip to the second one and send it
		Assert.assertNotEquals(wi1,  wi2);
		
		String export1 = format(wi1);
		String export2 = format(wi2); 
		verify(mockEdiExportService, timeout((int)(expectedRetryDelay * 1000L)).times(1)).sendWorkInstructionsToHost(eq(export1));
		verify(mockEdiExportService, timeout((int)(expectedRetryDelay * 1000L)).times(1)).sendWorkInstructionsToHost(eq(export2));

		this.getTenantPersistenceService().commitTransaction();
	}

	
	@Test
	public void workInstructionExportingIsNotBlocked() throws IOException, InterruptedException {
		this.getTenantPersistenceService().beginTransaction();
		
		final int total = 100;
		Lock callBlocker = new ReentrantLock();
		callBlocker.lock();
		final IEdiService mockEdiExportService = createBlockingService(callBlocker);
		doAnswer(new BlockedCall(callBlocker)).when(mockEdiExportService).sendWorkInstructionsToHost(any(String.class));

		createWorkService(total +1, mockEdiExportService, 1L);
		final WorkService workService = spy(this.workService);
		doCallRealMethod().when(workService).exportWorkInstruction(any(WorkInstruction.class));

		Facility facility = facilityGenerator.generateValid();
		for(int i = 0; i < total; i++) {
			WorkInstruction wi = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
			workService.exportWorkInstruction(wi);
		}

		verify(workService, Mockito.timeout(2000).times(total)).exportWorkInstruction(any(WorkInstruction.class));
		verify(mockEdiExportService, Mockito.times(1)).sendWorkInstructionsToHost(any(String.class));
		callBlocker.unlock();
		verify(mockEdiExportService, Mockito.timeout(2000).times(total)).sendWorkInstructionsToHost(any(String.class));
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void throwsExceptionWhenAtCapacity() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		int capacity = 10;
		int inflight = capacity + 1; //1 for the blocked work instruction
		int totalWorkInstructions = inflight + 1;
		List<WorkInstruction> wiList = generateValidWorkInstructions(totalWorkInstructions);

		Lock callBlocker = new ReentrantLock();
		callBlocker.lock();
		IEdiService blockingService = createBlockingService(callBlocker);
		createWorkService(capacity, blockingService, 1L);
		int exportAttempts = 0;
		for (WorkInstruction workInstruction : wiList) {
			try {
				workService.exportWorkInstruction(workInstruction);
				exportAttempts++;
				if (exportAttempts > inflight) {
					Assert.fail("Should have thrown an exception if capacity is reached");
				}
			}
			catch(IllegalStateException e) {

			}
		}
		callBlocker.unlock();
		this.getTenantPersistenceService().commitTransaction();
	}

	private void createWorkService(int capacity, IEdiService ediService, long retryDelay) {
		
		IEdiExportServiceProvider provider = mock(IEdiExportServiceProvider.class);
		when(provider.getWorkInstructionExporter(any(Facility.class))).thenReturn(ediService);

		this.workService = new WorkService(provider);
		this.workService.setCapacity(capacity);
		this.workService.setRetryDelay(retryDelay);		
		
		this.initializeEphemeralServiceManager();
	}
	
	private class TimedExceptionAnswer extends ThrowsException {
		private static final long	serialVersionUID	= 1L;
		private List<Long> timestamps;
		public TimedExceptionAnswer(Throwable throwable, List<Long> timestamps) {
			super(throwable);
			this.timestamps = timestamps;
		}

		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			this.timestamps.add(System.currentTimeMillis());
			return super.answer(invocation);
		}
	}

	private class TimedDoesNothing extends DoesNothing {
		private static final long	serialVersionUID	= 1L;
		private List<Long> timestamps;
		public TimedDoesNothing(List<Long> timestamps) {
			super();
			this.timestamps = timestamps;
		}

		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			this.timestamps.add(System.currentTimeMillis());
			return super.answer(invocation);
		}
	}

	private class BlockedCall extends DoesNothing {
		private static final long	serialVersionUID	= 1L;

		Lock callBlocker;
		public BlockedCall(Lock lock) {
			super();
			this.callBlocker = lock;
		}

		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			this.callBlocker.lock();
			this.callBlocker.unlock();
			System.out.println("Completed");
			return super.answer(invocation);
		}
	}

	private IEdiService createBlockingService(Lock callBlocker) throws IOException {
		final IEdiService mockEdiExportService = mock(IEdiService.class);
		doAnswer(new BlockedCall(callBlocker)).when(mockEdiExportService).sendWorkInstructionsToHost(any(String.class));
		return mockEdiExportService;
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
	
	private UUID firstChe(Facility facility) {
		UUID cheId = null;
		for(CodeshelfNetwork network : facility.getNetworks()) {
			for(Che che: network.getChes().values()) {
				cheId = che.getPersistentId();
				break;
			}
		}
		return cheId;
	}
}
