package com.codeshelf.behavior;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.codeshelf.manager.Tenant;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.UserContext;
import com.google.common.util.concurrent.SettableFuture;

public class TenantCallableTest {

	@Test
	public void testSimple() throws Exception {
		BatchProcessor singleLoop = mock(BatchProcessor.class); 
		when(singleLoop.doSetup()).thenReturn(2);
		when(singleLoop.isDone()).thenReturn(false, true);
		when(singleLoop.doBatch(any(Integer.class))).thenReturn(2);
		//when(singleLoop.doTeardown()).thenReturn(2);
		
		TenantCallable subject = new TenantCallable(mock(TenantPersistenceService.class), mock(Tenant.class), mock(UserContext.class), singleLoop);
		subject.call();
		// Bad assert here. Used say getTotalTime() > 0, but if test got faster (because of less foolish logging), then it failed.
		Assert.assertTrue(subject.getTotalTime() >= 0); // so now, really only testing that the stopwatch was initialized in the TenantCallable
		// The test as a whole is a valid exercise of the callable. Just not much to assert on.
	}
	
	@Test
	public void testCancel() throws Exception {
		final SettableFuture<Object> inDoBatch = SettableFuture.create();
		final SettableFuture<Object> waitForCancel = SettableFuture.create();
		
		BatchProcessor singleLoop = mock(BatchProcessor.class); 
		when(singleLoop.doSetup()).thenReturn(2);
		when(singleLoop.isDone()).thenReturn(false);
		when(singleLoop.doBatch(any(Integer.class))).thenAnswer(new Answer<Integer>() {

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				inDoBatch.set("here");
				try {
					waitForCancel.get(); //blocker for cancel should interrupt
					throw new RuntimeException("should not get here");
				} catch(InterruptedException e) {
					return 0;
				}
			}});

		
		TenantCallable subject = new TenantCallable(mock(TenantPersistenceService.class), mock(Tenant.class), mock(UserContext.class), singleLoop);
		Future<BatchReport> cancelledResult = Executors.newSingleThreadExecutor().submit(subject);
		inDoBatch.get(2, TimeUnit.SECONDS); //ensures we got to doBatch at least
		subject.cancel().get(2, TimeUnit.SECONDS);
		Assert.assertFalse("Callable should no longer be running after cancel returns", subject.isRunning());
		Assert.assertEquals(BatchReport.Status.CANCELLED, cancelledResult.get(2, TimeUnit.SECONDS).getStatus());

	}

	@Test
	public void testProgressReport() throws Exception {
		BatchProcessor loopThree = mock(BatchProcessor.class); 
		
		when(loopThree.doSetup()).thenReturn(5);
		when(loopThree.isDone()).thenReturn(false, false, false, true);
		when(loopThree.doBatch(any(Integer.class))).thenReturn(2, 4, 5);

		TenantPersistenceService mockPersistence = mock(TenantPersistenceService.class);
		TenantCallable subject = new TenantCallable(mockPersistence, mock(Tenant.class), mock(UserContext.class), loopThree);
		BatchReport result = subject.call();
		Assert.assertEquals(BatchReport.Status.COMPLETE, result.getStatus());
		Assert.assertEquals(5, result.getCompleteCount());
		Assert.assertEquals(5, result.getTotal());

		
		//ArgumentCaptor<Report> matcher = ArgumentCaptor.forClass(Report.class);
		//verify(mockPersistence, times(5)).saveOrUpdate(matcher.capture());
		
		/* callable saves same reference each time so can't see intermediate changes :(
		
		Iterator<Report> iterator = matcher.getAllValues().iterator();
		
		Report next = iterator.next();
		Assert.assertEquals(5, next.getTotal());
		Assert.assertEquals(0, next.getCount());
		Assert.assertEquals(Report.Status.INPROGRESS, next.getStatus());

		
		next = iterator.next();
		Assert.assertEquals(2, next.getCount());

		next = iterator.next();
		Assert.assertEquals(4, next.getCount());

		next = iterator.next();
		Assert.assertEquals(5, next.getCount());

		next = iterator.next();
		Assert.assertEquals(Report.Status.COMPLETE, next.getStatus());
		*/
	}
	
}
