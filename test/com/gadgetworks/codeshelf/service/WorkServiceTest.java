package com.gadgetworks.codeshelf.service;

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

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.edi.IEdiExportServiceProvider;
import com.gadgetworks.codeshelf.edi.WorkInstructionCSVExporter;
import com.gadgetworks.codeshelf.generators.FacilityGenerator;
import com.gadgetworks.codeshelf.generators.WorkInstructionGenerator;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.ConverterProvider;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ServiceMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ServiceMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.google.common.collect.ImmutableList;

public class WorkServiceTest extends DAOTestABC {
	
	static {
		Configuration.loadConfig("test");
	}
	
	private WorkInstructionGenerator wiGenerator = new WorkInstructionGenerator();
	private FacilityGenerator facilityGenerator = new FacilityGenerator();

	@Test
	public void shortedWorkInstructionShortsOrderDetail() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wiToRecord = generateValidWorkInstruction(facility, new Timestamp(0));
		UUID detailId = wiToRecord.getOrderDetail().getPersistentId();
		UUID cheId = wiToRecord.getAssignedChe().getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		
		wiToRecord.setStatus(WorkInstructionStatusEnum.SHORT);

		this.getPersistenceService().beginTenantTransaction();
		OrderDetail priorOrderDetail = OrderDetail.DAO.findByPersistentId(detailId);
		Assert.assertNotEquals(OrderStatusEnum.SHORT, priorOrderDetail.getStatus());

		WorkService workService = new WorkService().start();
		workService.completeWorkInstruction(cheId,	wiToRecord);
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		OrderDetail updatedOrderDetail = OrderDetail.DAO.findByPersistentId(detailId);
		Assert.assertEquals(OrderStatusEnum.SHORT, updatedOrderDetail.getStatus());
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void workSummaryRequest() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = facilityGenerator.generateValid();
		UUID cheId = firstChe(facility);
		
		ServiceMethodRequest request = new ServiceMethodRequest();
		request.setClassName("WorkService"); //the ux would use strings
		request.setMethodName("workAssignedSummary");
		request.setMethodArgs(ImmutableList.of(cheId.toString(), facility.getPersistentId().toString()));
		WorkService workService = mock(WorkService.class);
		when(workService.workAssignedSummary(eq(cheId), eq(facility.getPersistentId()))).thenReturn(Collections.<WiSetSummary>emptyList());
		ServiceFactory factory = new ServiceFactory(workService, mock(LightService.class), mock(PropertyService.class), mock(UiUpdateService.class));
		MessageProcessor processor = new ServerMessageProcessor(factory, new ConverterProvider().get());
		ResponseABC responseABC = processor.handleRequest(mock(UserSession.class), request);
		Assert.assertTrue(responseABC instanceof ServiceMethodResponse);
		Assert.assertTrue(responseABC.isSuccess());

		ServiceMethodRequest request2 = new ServiceMethodRequest();
		request2.setClassName("WorkService"); //the ux would use strings
		request2.setMethodName("workCompletedSummary");
		request2.setMethodArgs(ImmutableList.of(cheId.toString(), facility.getPersistentId().toString()));
		WorkService workService2 = mock(WorkService.class);
		when(workService2.workCompletedSummary(eq(cheId), eq(facility.getPersistentId()))).thenReturn(Collections.<WiSetSummary>emptyList());
		ServiceFactory factory2 = new ServiceFactory(workService2, mock(LightService.class), mock(PropertyService.class), mock(UiUpdateService.class));
		MessageProcessor processor2 = new ServerMessageProcessor(factory2, new ConverterProvider().get());
		ResponseABC responseABC2 = processor2.handleRequest(mock(UserSession.class), request2);
		Assert.assertTrue(responseABC2 instanceof ServiceMethodResponse);
		Assert.assertTrue(responseABC2.isSuccess());
	
	}
	
	

	@SuppressWarnings("unchecked")
	@Test
	public void summariesAreSorted() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = facilityGenerator.generateValid();

		ITypedDao<WorkInstruction> workInstructionDao = mock(ITypedDao.class);
		WorkInstruction.DAO = workInstructionDao;

		ArrayList<WorkInstruction> inputs = new ArrayList<WorkInstruction>();
		for (int i = 0; i < 4; i++) {
			inputs.add(generateValidWorkInstruction(facility, nextUniquePastTimestamp()));
		}
		when(workInstructionDao.findByFilter(anyList())).thenReturn(inputs);

		WorkService workService = new WorkService().start();
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

		workService.stop();
		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void completeWorkInstructionExceptionIfNotFound() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		UUID cheId = UUID.randomUUID();

		WorkService workService = createWorkService(Integer.MAX_VALUE, mock(IEdiService.class), 1L);
		WorkInstruction wiToRecord = generateValidWorkInstruction(facilityGenerator.generateValid(), new Timestamp(0));

		Che.DAO = mock(ITypedDao.class);
		when(Che.DAO.findByPersistentId(eq(cheId))).thenReturn(new Che());

		WorkInstruction.DAO = mock(ITypedDao.class);
		when(WorkInstruction.DAO.findByPersistentId(eq(wiToRecord.getPersistentId()))).thenReturn(null);

		try {
			workService.completeWorkInstruction(cheId, wiToRecord);
			Assert.fail("recordCompletedWorkInstruction should have thrown an exception if WI cannot be found");
		} catch (InputValidationException e) {
			Assert.assertNotNull(e.getErrors().getFieldErrors("persistentId"));
			Assert.assertFalse(e.getErrors().getFieldErrors("persistentId").isEmpty());
		}
		verify(WorkInstruction.DAO, never()).store(any(WorkInstruction.class));
		
		workService.stop();
		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void doesNotExportIfWICannotBeStored() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction existingWi = generateValidWorkInstruction(facility, new Timestamp(0));
		WorkInstruction wiToRecord = generateValidWorkInstruction(facility, new Timestamp(0));
		this.getPersistenceService().commitTenantTransaction();

		IEdiService mockEdiExportService = mock(IEdiService.class);
		WorkService workService = createWorkService(Integer.MAX_VALUE, mockEdiExportService, 1L);

		UUID cheId = UUID.randomUUID();
		Che.DAO = mock(ITypedDao.class);
		when(Che.DAO.findByPersistentId(eq(cheId))).thenReturn(new Che());

		UUID testId = UUID.randomUUID();

		existingWi.setPersistentId(testId);
		wiToRecord.setPersistentId(testId);

		WorkInstruction.DAO = mock(ITypedDao.class);
		OrderDetail.DAO = mock(ITypedDao.class);
		OrderHeader.DAO = mock(ITypedDao.class);
		when(WorkInstruction.DAO.findByPersistentId(eq(wiToRecord.getPersistentId()))).thenReturn(existingWi);

		doThrow(new DaoException("test")).when(WorkInstruction.DAO).store(eq(wiToRecord));

		workService.completeWorkInstruction(cheId, wiToRecord);

		verify(mockEdiExportService, never()).sendWorkInstructionsToHost(any(String.class));
		workService.stop();
	}

	@Test
	public void allWorkInstructionsSent() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		IEdiService mockEdiExportService = mock(IEdiService.class);

		int total = 100;
		WorkService workService = createWorkService(total+1, mockEdiExportService, 1L);
		List<WorkInstruction> wiList = generateValidWorkInstructions(total);
		for (WorkInstruction wi: wiList) {
			workService.exportWorkInstruction(wi);

		}

		verify(mockEdiExportService, Mockito.timeout(2000).times(total)).sendWorkInstructionsToHost(any(String.class));
		
		workService.stop();
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void workInstructionExportIsRetried() throws IOException, InterruptedException {
		this.getPersistenceService().beginTenantTransaction();

		long expectedRetryDelay = 1L;
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi = generateValidWorkInstruction(facility, nextUniquePastTimestamp());

		String messageBody = format(wi);
		
		IEdiService mockEdiExportService = mock(IEdiService.class);
		doThrow(new IOException("test io")).
		doThrow(new IOException("second one")).
		doNothing().when(mockEdiExportService).sendWorkInstructionsToHost(messageBody);

		WorkService workService = createWorkService(Integer.MAX_VALUE, mockEdiExportService, expectedRetryDelay);
		workService.exportWorkInstruction(wi);

		//Wait up to a second per invocation to verify
		verify(mockEdiExportService, Mockito.timeout((int)(expectedRetryDelay * 1000L)).times(3)).sendWorkInstructionsToHost(eq(messageBody));
		workService.stop();
		
		this.getPersistenceService().commitTenantTransaction();
	}

	private String format(WorkInstruction wi) throws IOException {
		WorkInstructionCSVExporter exporter = new WorkInstructionCSVExporter();
		// TODO Auto-generated method stub
		return exporter.exportWorkInstructions(ImmutableList.of(wi));
	}

	@Test
	public void workInstructionExportRetriesAreDelayed() throws IOException, InterruptedException {
		this.getPersistenceService().beginTenantTransaction();

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
		WorkService workService = createWorkService(Integer.MAX_VALUE, mockEdiExportService, expectedRetryDelay);
		workService.exportWorkInstruction(wi);

		verify(mockEdiExportService, Mockito.timeout((int)(expectedRetryDelay * 1000L)).times(3)).sendWorkInstructionsToHost(eq(testMessage));
		long previousTimestamp = timings.remove(0);
		for (Long timestamp : timings) {
			long diff = timestamp - previousTimestamp;
			Assert.assertTrue("The delay between calls was not greater than " + expectedRetryDelay + "ms but was: " + diff, diff > expectedRetryDelay);
		}

		workService.stop();
		
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void workInstructionContinuesOnRuntimeException() throws IOException, InterruptedException {
		this.getPersistenceService().beginTenantTransaction();

		long expectedRetryDelay = 1L;
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi1 = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
		WorkInstruction wi2 = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
		IEdiService mockEdiExportService = mock(IEdiService.class);
		doThrow(new RuntimeException("test io")).
		doNothing().when(mockEdiExportService).sendWorkInstructionsToHost(any(String.class));

		WorkService workService = createWorkService(Integer.MAX_VALUE, mockEdiExportService, expectedRetryDelay);
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

		this.getPersistenceService().commitTenantTransaction();
	}

	
	@Test
	public void workInstructionExportingIsNotBlocked() throws IOException, InterruptedException {
		this.getPersistenceService().beginTenantTransaction();
		
		final int total = 100;
		Lock callBlocker = new ReentrantLock();
		callBlocker.lock();
		final IEdiService mockEdiExportService = createBlockingService(callBlocker);
		doAnswer(new BlockedCall(callBlocker)).when(mockEdiExportService).sendWorkInstructionsToHost(any(String.class));

		final WorkService workService = spy(createWorkService(total +1, mockEdiExportService, 1L));
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
		
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void throwsExceptionWhenAtCapacity() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		int capacity = 10;
		int inflight = capacity + 1; //1 for the blocked work instruction
		int totalWorkInstructions = inflight + 1;
		List<WorkInstruction> wiList = generateValidWorkInstructions(totalWorkInstructions);

		Lock callBlocker = new ReentrantLock();
		callBlocker.lock();
		IEdiService blockingService = createBlockingService(callBlocker);
		WorkService workService = createWorkService(capacity, blockingService, 1L);
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
		workService.stop();
		this.getPersistenceService().commitTenantTransaction();
	}

	private WorkService createWorkService(int capacity, IEdiService ediService, long retryDelay) {
		IEdiExportServiceProvider provider = mock(IEdiExportServiceProvider.class);
		when(provider.getWorkInstructionExporter(any(Facility.class))).thenReturn(ediService);

		WorkService ws = new WorkService(provider);
		ws.setCapacity(capacity);
		ws.setRetryDelay(retryDelay);		
		ws.start();
		
		return ws;
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
