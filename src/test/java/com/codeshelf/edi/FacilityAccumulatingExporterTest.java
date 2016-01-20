package com.codeshelf.edi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.ScriptException;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FileExportReceipt;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.ExtensionPointEngine;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.testframework.HibernateTest;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class FacilityAccumulatingExporterTest extends HibernateTest {

	private static final Logger			LOGGER		= LoggerFactory.getLogger(FacilityAccumulatingExporterTest.class);

	private static final long	TERMINATION_SECONDS	= 5;

	private WorkInstructionGenerator	wiGenerator	= new WorkInstructionGenerator();
	private InventoryGenerator	inventoryGenerator = new InventoryGenerator(null);
	private FacilityGenerator			facilityGenerator;


	@Override
	public void doBefore() {
		super.doBefore();
		facilityGenerator = new FacilityGenerator();
	}
	
	private OrderHeader generateOrder(Facility facility, int numDetails) {
		
		OrderHeader orderHeader = wiGenerator.generateValidOrderHeader(facility);
		for (int i = 0; i < numDetails; i++) {
			wiGenerator.generateValidOrderDetail(orderHeader, inventoryGenerator.generateItem(facility));
		}
		return orderHeader;
	}
	
	private FacilityAccumulatingExporter startExporter(Facility facility, WiBeanStringifier stringifier, IEdiExportGateway exportGateway) throws TimeoutException {
		FacilityAccumulatingExporter subject = new FacilityAccumulatingExporter(facility);
		subject.setStringifier(stringifier);
		subject.setEdiExportGateway(exportGateway);
		subject.startAsync().awaitRunning(5, TimeUnit.SECONDS);
		return subject;
	}
	
	@Test 
	public void startUpImmediateShutdown() throws TimeoutException {
		beginTransaction();
		FacilityAccumulatingExporter subject = startExporter(getFacility(), mock(WiBeanStringifier.class), mock(IEdiExportGateway.class));
		commitTransaction();
		subject.exportOrderOnCartAdded(mock(OrderHeader.class), mock(Che.class));
		subject.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
	}
	
	@Test
	public void accumulatorTestWithExtensions() throws IOException, InterruptedException, ScriptException, TimeoutException {
		
		// What is a test with no asserts?  Not so great. Can see the output in the log/console, though. It works.

		LOGGER.info("1: Add the orderOnCart, header, trailer, and content extensions");

		// TODO change to order bean and che bean so we do not have to anticipate every need as a separate parameter. 
		// Never pass domain object to groovy.
		// Note: PFS web is both fixed field length, and delimited. First field is message ID, I think; not length.
		String onCartScript = "def OrderOnCartContent(bean) { def returnStr = " //
				+ "'0073' +'^'" //
				+ "+ 'ORDERSTATUS'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20) +'^'" //
				+ "+ bean.cheId.padRight(7) +'^'" //
				+ "+ bean.customerId.padRight(2) +'^'" //
				+ "+ 'OPEN'.padRight(15);" //
				+ " return returnStr;}";

		String headerScript = "def WorkInstructionExportCreateHeader(bean) { def returnStr = " //
				+ "'0073' +'^'" //
				+ "+ 'ORDERSTATUS'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20) +'^'" //
				+ "+ 'CHE'.padRight(20) +'^'" //
				+ "+ bean.cheId.padRight(20) +'^'" //
				+ "+ 'CLOSED'.padRight(15);" //
				+ " return returnStr;}";

		String trailerScript = "def WorkInstructionExportCreateTrailer(bean) { def returnStr = " //
				+ "'0057' +'^'" //
				+ "+ 'ENDORDER'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20);" //
				+ " return returnStr;}";

		// This matches the short specification. Customer sent a longer specification with timestamps and user names.
		String contentScript = "def WorkInstructionExportContent(bean) { def returnStr = " //
				+ "'0090' +'^'" //
				+ "+ 'PICKMISSIONSTATUS'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20) +'^'" //
				+ "+ bean.locationId.padRight(20) +'^'" //
				+ "+ bean.planQuantity.padLeft(15,'0') +'^'" //
				+ "+ bean.actualQuantity.padLeft(15,'0') +'^'" //
				+ "+ bean.itemId.padRight(25);" //
				+ " return returnStr;}";
 
		beginTransaction();
		Facility facility = facilityGenerator.generateValid();

		ExtensionPointEngine  extensionPointEngine = ExtensionPointEngine.getInstance(facility);
		// For PFSWeb (and Dematic carts), the OrderOnCart is approximately the same as the work instruction header, 
		// but this will not be universally true
		ExtensionPoint onCartExt = new ExtensionPoint(facility, ExtensionPointType.OrderOnCartContent);
		onCartExt.setScript(onCartScript);
		onCartExt.setActive(true);
		extensionPointEngine.create(onCartExt);

		ExtensionPoint headerExt = new ExtensionPoint(facility, ExtensionPointType.WorkInstructionExportCreateHeader);
		headerExt.setScript(headerScript);
		headerExt.setActive(true);
		extensionPointEngine.create(headerExt);

		ExtensionPoint trailerExt = new ExtensionPoint(facility, ExtensionPointType.WorkInstructionExportCreateTrailer);
		trailerExt.setScript(trailerScript);
		trailerExt.setActive(true);
		extensionPointEngine.create(trailerExt);

		ExtensionPoint contentExt = new ExtensionPoint(facility, ExtensionPointType.WorkInstructionExportContent);
		contentExt.setScript(contentScript);
		contentExt.setActive(true);
		extensionPointEngine.create(contentExt);

		LOGGER.info("1: Make the work instruction");
		
		Che che = firstChe(facility);
		OrderHeader orderHeader = generateOrder(facility, 2);
		List<WorkInstruction> wis = generateValidWorkInstruction(orderHeader, che, nextUniquePastTimestamp());
		commitTransaction();

		LOGGER.info("2: Create a phony EDI export service"); // needs an active transaction
		// This is a real-ish service with meaningful overrides.
		beginTransaction();
		
		WiBeanStringifier stringifier = new WiBeanStringifier(ExtensionPointEngine.getInstance(facility));
		IEdiExportGateway exportService = mock(IEdiExportGateway.class);
		FacilityAccumulatingExporter ediExporter = startExporter(facility, stringifier, exportService);
		
		LOGGER.info("3: notify order on cart"); // Use extension points
		ArrayList<ListenableFuture<ExportReceipt>> receipts = new ArrayList<ListenableFuture<ExportReceipt>>();
		receipts.add(ediExporter.exportOrderOnCartAdded(orderHeader, che));

		LOGGER.info("4: Accumulate the wi"); // this does not use extension points
		for (WorkInstruction workInstruction : wis) {
			workInstruction.setCompleteState("picker", workInstruction.getPlanQuantity());
			ediExporter.exportWiFinished(orderHeader, che, workInstruction);
		}
		LOGGER.info("5: Report on the order"); // Use extension points
		receipts.add(ediExporter.exportOrderOnCartFinished(orderHeader, che));

		Futures.allAsList(receipts);
		//TODO verify message
		verify(exportService).transportOrderOnCartAdded(eq(orderHeader.getDomainId()), eq(che.getDeviceGuidStr()), any(String.class));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class); 
		verify(exportService).transportOrderOnCartFinished(eq(orderHeader.getDomainId()), eq(che.getDeviceGuidStr()), messageCaptor.capture());
		String message = messageCaptor.getValue();
		assertMatches(orderHeader, che, wis, message);
		
		verify(exportService, never()).transportWiFinished(any(String.class), any(String.class), any(String.class));
		commitTransaction();
	}
	
	@Test
	public void workInstructionContinuesOnRuntimeException() throws IOException, InterruptedException, TimeoutException {
		beginTransaction();

		long expectedRetryDelay = 1L;
		Facility facility = getFacility();
		Che che1 = getChe1();
		Che che2 = getChe2();
		
		OrderHeader order1 = wiGenerator.generateValidOrderHeader(facility);
		wiGenerator.generateValidOrderDetail(order1, inventoryGenerator.generateItem(facility));
		OrderHeader order2 = wiGenerator.generateValidOrderHeader(facility);
		wiGenerator.generateValidOrderDetail(order2, inventoryGenerator.generateItem(facility));
		OrderHeader order3 = wiGenerator.generateValidOrderHeader(facility);
		wiGenerator.generateValidOrderDetail(order3, inventoryGenerator.generateItem(facility));

		Assert.assertNotEquals(order1, order2);
		Assert.assertNotEquals(order2, order3);
		Assert.assertNotEquals(che1, che2);

		
		
		IEdiExportGateway mockEdiTransport = mock(IEdiExportGateway.class);

		FacilityAccumulatingExporter subject = startExporter(facility, mock(WiBeanStringifier.class), mockEdiTransport);
		try {
			beginTransaction();
			when(mockEdiTransport.transportOrderOnCartAdded(any(String.class), any(String.class), any(String.class)))
			.thenThrow(new RuntimeException("test io"))
			.thenReturn(mock(FileExportReceipt.class));
	
			doThrow(new RuntimeException("test io")).doNothing()
				.when(mockEdiTransport)
				.transportOrderOnCartRemoved(any(String.class), any(String.class), any(String.class));
	
			doThrow(new RuntimeException("test io")).doNothing()
				.when(mockEdiTransport)
				.transportWiFinished(any(String.class), any(String.class), any(String.class));
	
			when(mockEdiTransport.transportOrderOnCartFinished(any(String.class), any(String.class), any(String.class)))
			.thenThrow(new RuntimeException("test io"))
			.thenReturn(mock(FileExportReceipt.class));
	
			subject.exportOrderOnCartAdded(order1, che1);
			subject.exportOrderOnCartAdded(order2, che1);
			subject.exportOrderOnCartAdded(order3, che2);
			
			//subject.exportOrderOnCartRemoved(order1, che1);
			//subject.exportOrderOnCartRemoved(order2, che1);
			//subject.exportOrderOnCartRemoved(order3, che2);
	
			subject.exportWiFinished(order1, che1, generateValidWorkInstruction(order1, che1, nextUniquePastTimestamp()).get(0));
			subject.exportWiFinished(order2, che1, generateValidWorkInstruction(order2, che1, nextUniquePastTimestamp()).get(0));
			subject.exportWiFinished(order3, che2, generateValidWorkInstruction(order3, che2, nextUniquePastTimestamp()).get(0));
	
			subject.exportOrderOnCartFinished(order1, che1);
			subject.exportOrderOnCartFinished(order2, che1);
			subject.exportOrderOnCartFinished(order3, che2);
	
			//Wait up to a second per invocation to verify
			// Normal behavior is to keep retrying on IOException but skip on runtime exception
			//   in this case it should skip to the second one and send it
			verify(mockEdiTransport, timeout((int) (expectedRetryDelay * 1000L)).times(3)).transportOrderOnCartAdded(any(String.class), any(String.class), any(String.class));
			//not called for PFSweb
			//verify(mockEdiTransport, timeout((int) (expectedRetryDelay * 1000L)).times(3)).transportOrderOnCartRemoved(any(OrderHeader.class), any(Che.class), any(String.class));
			verify(mockEdiTransport, timeout((int) (expectedRetryDelay * 1000L)).times(3)).transportOrderOnCartFinished(any(String.class), any(String.class), any(String.class));
			//not called for PFSweb
			//verify(mockEdiTransport, timeout((int) (expectedRetryDelay * 1000L)).times(3)).transportWiFinished(any(OrderHeader.class), any(Che.class), any(String.class));
			commitTransaction();
		} finally {
			subject.stopAsync().awaitTerminated(TERMINATION_SECONDS, TimeUnit.SECONDS);
		}
		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void workInstructionExportIsRetried() throws IOException, InterruptedException, TimeoutException {
		beginTransaction();

		long expectedRetryDelay = 1L;
		Facility facility = getFacility();
		Che che1 = getChe1();
		
		OrderHeader order1 = wiGenerator.generateValidOrderHeader(facility);

		String singleTestMessage = "ONLY MESSAGE RETRIED SEVERAL TIMES";
		WiBeanStringifier mockStringifier = mock(WiBeanStringifier.class);
		when(mockStringifier.stringifyOrderOnCartAdded(order1, che1)).thenReturn(singleTestMessage);
		
		IEdiExportGateway mockEdiTransport = mock(IEdiExportGateway.class);
		when(mockEdiTransport.transportOrderOnCartAdded(any(String.class), any(String.class), any(String.class)))
			.thenThrow(new IOException("test io"))
			.thenThrow(new IOException("test io 2"))
			.thenReturn(null);

		FacilityAccumulatingExporter subject = startExporter(facility, mockStringifier, mockEdiTransport);
		try {
			subject.exportOrderOnCartAdded(order1, che1);
			
			
			//Wait up to a second per invocation to verify
			verify(mockEdiTransport, timeout((int) (expectedRetryDelay * 5000L)).times(3)).transportOrderOnCartAdded(eq(order1.getDomainId()), eq(che1.getDeviceGuidStr()), eq(singleTestMessage));
	
			when(mockStringifier.stringifyOrderOnCartFinished(eq(order1), eq(che1), any(List.class))).thenReturn(singleTestMessage);
	
			when(mockEdiTransport.transportOrderOnCartFinished(any(String.class), any(String.class), any(String.class)))
			.thenThrow(new IOException("test io"))
			.thenThrow(new IOException("test io 2"))
			.thenReturn(null);
	
			subject.exportOrderOnCartFinished(order1, che1);
			verify(mockEdiTransport, timeout((int) (expectedRetryDelay * 5000L)).times(3)).transportOrderOnCartFinished(eq(order1.getDomainId()), eq(che1.getDeviceGuidStr()), eq(singleTestMessage));
		} finally {
			subject.stopAsync().awaitTerminated(TERMINATION_SECONDS, TimeUnit.SECONDS);
		}
		commitTransaction();
	}

	@Ignore
	@Test
	public void workInstructionExportingIsNotBlocked() throws IOException, InterruptedException, TimeoutException {

		this.getTenantPersistenceService().beginTransaction();

		final int total = 30;
		Facility facility = getFacility();
		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();
		facility = facility.reload();
		final Che che1 = getChe1();


		Lock callBlocker = new ReentrantLock();
		IEdiExportGateway mockEdiTransport = mock(IEdiExportGateway.class);
		when(mockEdiTransport.transportOrderOnCartAdded(any(String.class), any(String.class), any(String.class)))
			.thenAnswer(new BlockedCall(callBlocker, new FileExportReceipt("../../*.DAT", 500)));

		String singleTestMessage = "ONLY MESSAGE NOT BLOCKED";
		WiBeanStringifier mockStringifier = mock(WiBeanStringifier.class);
		when(mockStringifier.stringifyOrderOnCartAdded(any(OrderHeader.class), eq(che1))).thenReturn(singleTestMessage);

		final FacilityAccumulatingExporter subject = startExporter(facility, mockStringifier, mockEdiTransport);
		try {
			//block all transports
			callBlocker.lock();
	
	
			//Simulate multiple thread trying to send messages from several carts while the export transport is blocked
			ExecutorService executor = Executors.newFixedThreadPool(3);
			for (int i = 0; i < total; i++) {
				final OrderHeader order = wiGenerator.generateValidOrderHeader(facility);
				executor.execute(new Runnable() {
	
					@Override
					public void run() {
						subject.exportOrderOnCartAdded(order, che1);
					}
					
					
				});
			}
			executor.shutdown();
			Assert.assertTrue("Threads requesting export should not have been blocked", executor.awaitTermination(5L, TimeUnit.SECONDS));
	
	
			//verify(subject, Mockito.timeout(2000).times(total)).exportWorkInstruction(any(WorkInstruction.class));
	
			
			//verify(mockEdiTransport, Mockito.times(1)).transportOrderOnCartAdded(any(OrderHeader.class), any(Che.class), any(String.class));
			callBlocker.unlock();
			verify(mockEdiTransport, Mockito.timeout(2000).times(total)).transportOrderOnCartAdded(any(String.class), any(String.class), any(String.class));
		} finally {
			subject.stopAsync().awaitTerminated(TERMINATION_SECONDS, TimeUnit.SECONDS);
		}
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void testEdiTransportFixable() throws IOException, TimeoutException, InterruptedException, ExecutionException {
		IEdiExportGateway badEdiTransport = mock(IEdiExportGateway.class, new ThrowsException(new IOException("badly configured")));
		//when(badEdiTransport.transportOrderOnCartAdded(any(OrderHeader.class), any(Che.class), any(String.class)))
		//	.thenThrow(new IOException("badly configured"));

		String singleTestMessage = "MESSAGE SEND";
		WiBeanStringifier mockStringifier = mock(WiBeanStringifier.class, new Returns(singleTestMessage));
//		when(mockStringifier.stringifyOrderOnCartAdded(any(OrderHeader.class), any(Che.class))).thenReturn(singleTestMessage);

		beginTransaction();
		Facility facility = getFacility();
		final FacilityAccumulatingExporter subject = startExporter(facility, mockStringifier, badEdiTransport);
		try {
			OrderHeader mockOrder = mock(OrderHeader.class);
			Che mockChe = mock(Che.class);
			ListenableFuture<ExportReceipt> futureReceipt = subject.exportOrderOnCartAdded(mockOrder, mockChe);
			
			//test that it is not complete
			try {
				futureReceipt.get(2, TimeUnit.SECONDS);
				Assert.fail("Should not have gotten a result back");
			} catch(Exception e) {
				
			}
			//assert transport has been called at least once though not complete
			//verify(badEdiTransport, Mockito.atLeastOnce()).transportOrderOnCartAdded(eq(mockOrder), eq(mockChe), eq(singleTestMessage));
			Assert.assertFalse(futureReceipt.isDone());
			
			
			FileExportReceipt successReceipt = mock(FileExportReceipt.class);
			IEdiExportGateway goodEdiGateway = mock(IEdiExportGateway.class);
			//when(goodEdiTransport.transportOrderOnCartAdded(eq(mockOrder.getDomainId()), eq(mockChe.getDeviceGuidStr()), eq(singleTestMessage))).thenReturn(successReceipt);
			when(goodEdiGateway.transportOrderOnCartAdded(any(String.class), any(String.class), eq(singleTestMessage))).thenReturn(successReceipt);
			subject.setEdiExportGateway(goodEdiGateway);
			
			Assert.assertEquals(successReceipt, futureReceipt.get(5, TimeUnit.SECONDS));
		} finally {
			subject.stopAsync().awaitTerminated(TERMINATION_SECONDS, TimeUnit.SECONDS);
			commitTransaction();
		}
	}
	
	
	private List<WorkInstruction> generateValidWorkInstruction(OrderHeader orderHeader, Che che, Timestamp timestamp) {
		List<WorkInstruction> wis = new ArrayList<>();
		for (OrderDetail orderDetail : orderHeader.getOrderDetails()) {
			WorkInstruction wi = wiGenerator.generateWithNewStatus(orderDetail, che);
			wi.setAssigned(timestamp);
			wis.add(wi);
		}
		return wis;
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
	
	private Timestamp nextUniquePastTimestamp() {
		return new Timestamp(System.currentTimeMillis() - Math.abs(RandomUtils.nextLong()));
	}

	
	/*Assert.assertEquals(2, orderHeader.getOrderDetails().size());
	
	List<WorkInstruction> computedWIs = setupComputedWorkInstructionsForOrder(orderHeader, che);*/

	/**
	 * Very broad matcher
	 */
	private void assertMatches(OrderHeader completeOrder, Che che, List<WorkInstruction> wis, String message) {
		Assert.assertNotNull(Strings.emptyToNull(completeOrder.getOrderId()));
		Assert.assertTrue("Message did not contain order id: " + completeOrder.getOrderId(), message.contains(completeOrder.getOrderId()));

		Assert.assertNotNull(Strings.emptyToNull(che.getDomainId()));
		Assert.assertTrue("Message did not contain che id: " + che.getDomainId(), message.contains(che.getDomainId()));
		
		for (WorkInstruction workInstruction : wis) {
			Assert.assertTrue("Message did not contain item id: " + workInstruction.getItemId(), message.contains(workInstruction.getItemId()));
			Assert.assertTrue("Message did not contain item id: " + workInstruction.getActualQuantity(), message.contains(String.valueOf(workInstruction.getActualQuantity())));
			
		}
	}
	
	
	private class BlockedCall extends Returns {
		private static final long	serialVersionUID	= 1L;

		Lock						callBlocker;

		public BlockedCall(Lock lock, ExportReceipt result) {
			super(result);
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
	
	/*

	private List<WorkInstruction> setupComputedWorkInstructionsForOrder(OrderHeader orderHeader, Che che) {
		ArrayList<WorkInstruction> wis = new ArrayList<>();
		for (OrderDetail  orderDetail : orderHeader.getOrderDetails()) {
			WorkInstruction wi = wiGenerator.generateWithNewStatus(orderDetail, che);
			wis.add(wi);
			
		}
		return wis;
		
	}
	 
	 	@Test
	public void workInstructionExportRetriesAreDelayed() throws IOException, InterruptedException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
		String testMessage = format(wi);

		IEdiExportService mockEdiExportService = mock(IEdiExportService.class);

		List<Long> timings = new ArrayList<Long>();
		doAnswer(new TimedExceptionAnswer(new IOException("test io"), timings)).doAnswer(new TimedExceptionAnswer(new IOException("test io"),
			timings))
			.doAnswer(new TimedDoesNothing(timings))
			.when(mockEdiExportService)
			.sendWorkInstructionsToHost(testMessage);

		long expectedRetryDelay = 2000;
		createWorkService(Integer.MAX_VALUE, mockEdiExportService, expectedRetryDelay);
		workService.exportWorkInstruction(wi);

		verify(mockEdiExportService, Mockito.timeout((int) (expectedRetryDelay * 1000L)).times(3)).sendWorkInstructionsToHost(eq(testMessage));
		List<Long> timingsCopy = new ArrayList<Long>(timings);//copy as the calls may still modify timings if delayed
		long previousTimestamp = timingsCopy.remove(0);

		for (Long timestamp : timingsCopy) {
			long diff = timestamp - previousTimestamp;
			// change from diff > expectedRetryDelay to diff >= expectedRetryDelay. Not necessarily accurate to the ms, but JRs Mac caught this a lot.
			Assert.assertTrue("The delay between calls was not greater than " + expectedRetryDelay + "ms but was: " + diff,
				diff >= expectedRetryDelay);
		}

		this.getTenantPersistenceService().commitTransaction();
	}



	 
	 
	@Test
	public void workInstructionExportingIsNotBlocked() throws IOException, InterruptedException {

		this.getTenantPersistenceService().beginTransaction();

		final int total = 100;
		Lock callBlocker = new ReentrantLock();
		callBlocker.lock();
		final IEdiExportService mockEdiExportService = createBlockingService(callBlocker);
		doAnswer(new BlockedCall(callBlocker)).when(mockEdiExportService).sendWorkInstructionsToHost(any(String.class));

		createWorkService(total + 1, mockEdiExportService, 1L);
		final WorkService workService = spy(this.workService);
		doCallRealMethod().when(workService).exportWorkInstruction(any(WorkInstruction.class));

		Facility facility = facilityGenerator.generateValid();
		for (int i = 0; i < total; i++) {
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
		IEdiExportService blockingService = createBlockingService(callBlocker);
		createWorkService(capacity, blockingService, 1L);
		int exportAttempts = 0;
		for (WorkInstruction workInstruction : wiList) {
			try {
				workService.exportWorkInstruction(workInstruction);
				exportAttempts++;
				if (exportAttempts > inflight) {
					Assert.fail("Should have thrown an exception if capacity is reached");
				}
			} catch (IllegalStateException e) {

			}
		}
		callBlocker.unlock();
		this.getTenantPersistenceService().commitTransaction();
	}

	
		
	private List<WorkInstruction> generateValidWorkInstruction(OrderHeader orderHeader, Che che, Timestamp timestamp) {
		List<WorkInstruction> wis = new ArrayList<>();
		for (OrderDetail orderDetail : orderHeader.getOrderDetails()) {
			WorkInstruction wi = wiGenerator.generateWithNewStatus(orderDetail, che);
			wi.setAssigned(timestamp);
			wis.add(wi);
		}
		return wis;
	}

	private class TimedDoesNothing extends DoesNothing {
		private static final long	serialVersionUID	= 1L;
		private List<Long>			timestamps;

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

		Lock						callBlocker;

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
	
	private class TimedExceptionAnswer extends ThrowsException {
		private static final long	serialVersionUID	= 1L;
		private List<Long>			timestamps;

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


	private IEdiExportService createBlockingService(Lock callBlocker) throws IOException {
		final IEdiExportService mockEdiExportService = mock(IEdiExportService.class);
		doAnswer(new BlockedCall(callBlocker)).when(mockEdiExportService).sendWorkInstructionsToHost(any(String.class));
		return mockEdiExportService;
	}
*/	
}
