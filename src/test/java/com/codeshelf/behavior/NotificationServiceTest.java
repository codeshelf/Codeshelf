package com.codeshelf.behavior;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.api.responses.PickRate;
import com.codeshelf.behavior.NotificationBehavior.WorkerEventTypeGroup;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.testframework.HibernateTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class NotificationServiceTest extends HibernateTest {

	private DateTime eventTime = new DateTime(1955, 11, 12, 10, 04, 00, 00, DateTimeZone.forID("US/Central"));  //lightning will strike the clock tower in back to the future

	@Test
	public void hourlyRatesGroupedByEventType() {
		this.getTenantPersistenceService().beginTransaction();
		NotificationBehavior behavior = new NotificationBehavior();
		Che che = getTestChe();
		behavior.saveEvent(createEvent(eventTime, WorkerEvent.EventType.COMPLETE, che));
		behavior.saveEvent(createEvent(eventTime, WorkerEvent.EventType.SHORT, che));
		behavior.saveEvent(createEvent(eventTime.plus(1), WorkerEvent.EventType.COMPLETE, che));
		behavior.saveEvent(createEvent(eventTime.plus(1), WorkerEvent.EventType.SHORT, che));
		Interval betweenTimes = new Interval(eventTime.minus(1), eventTime.plus(2)); 
		List<PickRate> pickRates = behavior.getPickRate(ImmutableSet.of(WorkerEvent.EventType.COMPLETE, WorkerEvent.EventType.SHORT), ImmutableSet.of("workerId", "eventType"), betweenTimes);
		Assert.assertEquals(2, pickRates.size());
		for (PickRate pickRate : pickRates) {
			Assert.assertEquals(2, pickRate.getPicks().intValue());
		}
		this.getTenantPersistenceService().commitTransaction();
				
	}

	@Test
	public void hourlyRatesGroupedByPurposeEventType() {
		this.getTenantPersistenceService().beginTransaction();
		NotificationBehavior behavior = new NotificationBehavior();
		Che che = getTestChe();
		Facility facility = getFacility();
		Set<EventType> types = ImmutableSet.of(WorkerEvent.EventType.COMPLETE, WorkerEvent.EventType.SHORT, WorkerEvent.EventType.SKIP_ITEM_SCAN);
		Set<WiPurpose> purposes= ImmutableSet.of(WiPurpose.WiPurposeOutboundPick, WiPurpose.WiPurposePutWallPut);
		Set<Worker> workers = ImmutableSet.of(createWorker(facility, "badge1"), createWorker(facility, "badge2"));
		@SuppressWarnings("unchecked")
		Set<List<Object>> combos = Sets.cartesianProduct(types, purposes, workers);
		int count = 0;
		for (List<Object> combo : combos) {
			behavior.saveEvent(createEvent(eventTime.plus(count++), 
											(WorkerEvent.EventType)combo.get(0),
											 (WiPurpose)combo.get(1),
											 (Worker) combo.get(2),
											 che));
			
		}
		Interval betweenTimes = new Interval(eventTime.minus(1), eventTime.plus(count + 1)); 
		List<PickRate> pickRates = behavior.getPickRate(types, ImmutableSet.of("workerId", "eventType", "purpose"), betweenTimes);
		Assert.assertEquals(combos.size(), pickRates.size());
		for (PickRate pickRate : pickRates) {
			Assert.assertEquals(1, pickRate.getPicks().intValue());
		}
		this.getTenantPersistenceService().commitTransaction();
				
	}

	
	
	@Test
	public void testWorkerEventsGroupByType() {
		this.getTenantPersistenceService().beginTransaction();
		Che che = getTestChe();
		
		NotificationBehavior behavior = new NotificationBehavior();
		DateTime eventTime2 = new DateTime(eventTime.getMillis() + 5);
		DateTime eventTime3 = new DateTime(eventTime.getMillis() + 10);
		DateTime eventTime4 = new DateTime(eventTime.getMillis() + 15);
		behavior.saveEvent(createEvent(eventTime, WorkerEvent.EventType.COMPLETE, che));
		behavior.saveEvent(createEvent(eventTime2, WorkerEvent.EventType.COMPLETE, che));
		behavior.saveEvent(createEvent(eventTime3, WorkerEvent.EventType.SHORT, che));
		behavior.saveEvent(createEvent(eventTime4, WorkerEvent.EventType.SKIP_ITEM_SCAN, che));
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		List<WorkerEventTypeGroup> groupedCounts = behavior.groupWorkerEventsByType(getFacility(), new Interval(eventTime.minus(1), eventTime4.plus(1)), false);
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
		NotificationBehavior behavior = new NotificationBehavior();
		storePickEvent(behavior, che, eventTime);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		List<PickRate> pickRates = behavior.getPickRate(ImmutableSet.of(EventType.COMPLETE, EventType.SHORT), ImmutableSet.of("workerId"), new Interval(startTime, endTime));
		Assert.assertEquals(numResults, pickRates.size());
		this.getTenantPersistenceService().commitTransaction();
		
	}

	private WorkerEvent createEvent(DateTime eventTime, WorkerEvent.EventType eventType, Che che) {
		
		WorkerEvent event = new WorkerEvent(eventTime, eventType, che, "worker");

		WorkInstruction wi = createShortWorkInstruction(che, "worker");
		WorkInstruction.staticGetDao().store(wi);
		WorkInstruction persistedWI = WorkInstruction.staticGetDao().findByDomainId(wi.getParent(), wi.getDomainId());
		event.setWorkInstructionId(persistedWI.getPersistentId());
		event.setLocation(persistedWI.getPickInstruction());
		return event;
	}
	private WorkerEvent createEvent(DateTime eventTime, EventType eventType, WiPurpose wiPurpose, Worker worker, Che che) {
		WorkerEvent event = new WorkerEvent(eventTime, eventType, che, worker.getDomainId());
		event.setPurpose(wiPurpose.name());
		return event;
	}

	private Worker createWorker(Facility facility, String domainId) {
		Worker worker = new Worker(facility, domainId);
		Worker.staticGetDao().store(worker);
		return worker;
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
