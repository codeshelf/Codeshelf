package com.codeshelf.behavior;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.api.responses.PickRate;
import com.codeshelf.behavior.NotificationBehavior.WorkerEventTypeGroup;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.testframework.HibernateTest;
import com.google.common.collect.ImmutableMap;

public class NotificationServiceTest extends HibernateTest {

	private DateTime eventTime = new DateTime(1955, 11, 12, 10, 04, 00, 00, DateTimeZone.forID("US/Central"));  //lightning will strike the clock tower in back to the future
	
	@Test
	public void testGroupByType() {
		DateTime eventTime2 = new DateTime(eventTime.getMillis() + 5);
		DateTime eventTime3 = new DateTime(eventTime.getMillis() + 10);
		DateTime eventTime4 = new DateTime(eventTime.getMillis() + 15);
		this.getTenantPersistenceService().beginTransaction();
		Che che = getTestChe();
		
		NotificationBehavior service = new NotificationBehavior();
		service.saveEvent(createEvent(eventTime, WorkerEvent.EventType.COMPLETE, che));
		service.saveEvent(createEvent(eventTime2, WorkerEvent.EventType.COMPLETE, che));
		service.saveEvent(createEvent(eventTime3, WorkerEvent.EventType.SHORT, che));
		service.saveEvent(createEvent(eventTime4, WorkerEvent.EventType.SKIP_ITEM_SCAN, che));
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		List<WorkerEventTypeGroup> groupedCounts = service.groupWorkerEventsByType(getFacility(), new Interval(eventTime.minus(1), eventTime4.plus(1)), false);
		Assert.assertEquals(3, groupedCounts.size());
		Map<WorkerEvent.EventType, Long> expectedValues = ImmutableMap.of(
			WorkerEvent.EventType.COMPLETE, 2L,
			WorkerEvent.EventType.SHORT, 1L,
			WorkerEvent.EventType.SKIP_ITEM_SCAN, 1L
		);
		
		
		for (WorkerEventTypeGroup workerEventTypeGroup : groupedCounts) {
			Assert.assertEquals(expectedValues.get(workerEventTypeGroup.getEventType()).longValue(), workerEventTypeGroup.getCount());
		}
		this.getTenantPersistenceService().commitTransaction();
	}
	
	
	@Test
	public void testMessageOnStartDate() {
		DateTime startTime = eventTime;
		DateTime endTime = eventTime.plus(1);
		testDateBoundaries(eventTime, startTime, endTime, 1);
		
	}

	@Test
	public void testMessageOnEndDate() {
		DateTime startTime = eventTime.minus(1);
		DateTime endTime = eventTime;
		testDateBoundaries(eventTime, startTime, endTime, 1);
	}

	
	@Test
	public void testMessageWithinDates() {
		DateTime startTime = eventTime.minus(1);
		DateTime endTime = eventTime.plus(1);
		testDateBoundaries(eventTime, startTime, endTime, 1);
	}

	@Test
	public void testMessageOutsideStartDate() {
		DateTime startTime = eventTime.plus(1);
		DateTime endTime = eventTime.plus(2);
		testDateBoundaries(eventTime, startTime, endTime, 0);
		
	}

	@Test
	public void testMessageOutsideEndDate() {
		DateTime startTime = eventTime.minus(2);
		DateTime endTime = eventTime.minus(1);
		testDateBoundaries(eventTime, startTime, endTime, 0);
		
	}
	
	private Che getTestChe() {
		getFacility();
		Che che = getChe1();
		return che;
	}
	
	private void testDateBoundaries(DateTime eventTime, DateTime startTime, DateTime endTime, int numResults) {
		this.getTenantPersistenceService().beginTransaction();
		Che che = getTestChe();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		NotificationBehavior service = new NotificationBehavior();
		storePickEvent(service, che, eventTime);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		List<PickRate> pickRates = service.getPickRate(startTime, endTime);
		Assert.assertEquals(numResults, pickRates.size());
		this.getTenantPersistenceService().commitTransaction();
		
	}

	private WorkerEvent createEvent(DateTime eventTime, WorkerEvent.EventType eventType, Che che) {
		
		WorkerEvent event = new WorkerEvent(eventTime, eventType, che, "worker");

		WorkInstruction wi = createShortWorkInstruction(che, "worker");
		WorkInstruction.staticGetDao().store(wi);
		WorkInstruction persistedWI = WorkInstruction.staticGetDao().findByDomainId(wi.getParent(), wi.getDomainId());
		event.setWorkInstruction(persistedWI);
		return event;
	}

	private WorkInstruction createShortWorkInstruction(Che inChe, String inWorkerId) {
		WorkInstructionGenerator wiGenerator = new WorkInstructionGenerator();
		WorkInstruction  wi = wiGenerator.generateValid(inChe.getFacility());
		wi.setAssignedChe(inChe);
		wi.setShortState(inWorkerId, 0);
		return wi;
	}

	
	private void storePickEvent(NotificationBehavior service, Che che, DateTime eventTime) {
		WorkerEvent event = createEvent(eventTime, WorkerEvent.EventType.COMPLETE, che);
		service.saveEvent(event);
	}
}
