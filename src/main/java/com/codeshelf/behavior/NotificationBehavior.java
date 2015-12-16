package com.codeshelf.behavior;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.responses.PickRate;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class NotificationBehavior implements IApiBehavior{
	
	private static final Logger			LOGGER				= LoggerFactory.getLogger(NotificationBehavior.class);
	
	private static final EnumSet<WorkerEvent.EventType>	SAVE_ONLY	= EnumSet.of(EventType.SKIP_ITEM_SCAN,
																		EventType.SHORT,
																		EventType.COMPLETE,
																		EventType.DETAIL_WI_MISMATCHED,
																		EventType.PALLETIZER_PUT,
																		EventType.PUTWALL_PUT,
																		EventType.SKUWALL_PUT,
																		EventType.LOW);
	
	private final WorkerHourlyMetricBehavior	workerHourlyMetricBehavior = new WorkerHourlyMetricBehavior();
	
	@Inject
	public NotificationBehavior() {
	}
	
	public WorkerEvent saveEvent(WorkerEvent event) {
		WorkerEvent.staticGetDao().store(event);
		return event;
	}

	public void saveFinishedWI(WorkInstruction wi) {
		EventType type = null;
		if (wi.getStatus().equals(WorkInstructionStatusEnum.COMPLETE)) {
			type = EventType.COMPLETE;
		} else if(wi.getStatus().equals(WorkInstructionStatusEnum.SHORT))  {
			type = EventType.SHORT;
		} else {
			return;
		}
		if (!SAVE_ONLY.contains(type)) {
			return;
		}
	
		LOGGER.info("Saving WorkerEvent {} from {} for {}", type, wi.getAssignedChe(), wi.getPickerId());
		WorkerEvent event = new WorkerEvent();
		Che device = wi.getAssignedChe();
		event.setDeviceGuid(device.getDeviceGuidStr());
		event.setParent(device.getFacility());
		event.setDevicePersistentId(device.getPersistentId().toString());
		
		event.setPurpose(wi.getPurpose().name());
		event.setCreated(wi.getCompleted());
		event.setEventType(type);
		event.setWorkerId(wi.getPickerId());
		event.setWorkInstructionId(wi.getPersistentId());
		event.setLocation(wi.getPickInstruction());
		OrderDetail orderDetail = wi.getOrderDetail();
		if (orderDetail != null) {
			event.setOrderDetailId(orderDetail.getPersistentId());
		}
		event.generateDomainId();
		WorkerEvent existingEventWithSameDomain = WorkerEvent.staticGetDao().findByDomainId(device.getFacility(), event.getDomainId());
		if (existingEventWithSameDomain == null){
			WorkerEvent.staticGetDao().store(event);
			//Save Complete or Short event into WorkerHourlyMetric
			workerHourlyMetricBehavior.recordEvent(wi.getFacility(), wi.getPickerId(), type);			
		} else {
			LOGGER.warn("Event " + event.getDomainId() + " already exists. Not creating another one.");
		}
	}
	
	public void saveEvent(NotificationMessage message) {
		if (!SAVE_ONLY.contains(message.getEventType())) {
			return;
		}
		boolean save_completed=false;
		try {
			TenantPersistenceService.getInstance().beginTransaction();
			LOGGER.info("Saving WorkerEvent {} from {} for {}", message.getEventType(), message.getNetGuidStr(), message.getWorkerId());
			WorkerEvent event = new WorkerEvent();

			Class<?> deviceClass = message.getDeviceClass();
			DomainObjectABC device = null;
			if (deviceClass == Che.class) {
				device = Che.staticGetDao().findByPersistentId(message.getDevicePersistentId());
			} else {
				throw new IllegalArgumentException("unable to process notifications from " + deviceClass + " devices");
			}
			if (device == null) {
				throw new IllegalArgumentException(String.format("unable to find %s device %s (%s)", deviceClass, message.getDevicePersistentId(), message.getNetGuidStr()));
			}
			event.setDeviceGuid(new NetGuid(message.getNetGuidStr()).toString());
			event.setParent(device.getFacility());
			event.setDevicePersistentId(device.getPersistentId().toString());
			
			event.setCreated(new Timestamp(message.getTimestamp()));
			event.setEventType(message.getEventType());
			event.setWorkerId(message.getWorkerId());
			
			UUID workInstructionId = message.getWorkInstructionId();
			if (workInstructionId != null) {
				WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(workInstructionId);
				// It is possible, though bad and unlikely, that server has deleted a work instruction by the time site controller sends message to server about it.
				// This dataflow is seen in DataArchiving.testPurgeActiveJobs()
				if (wi == null) {
					throw new NullPointerException(String.format("Work instruction does not exist for this persistentId: %s", workInstructionId.toString()));
					// would get NPE just below in wi.getOrderDetail();
				}
				event.setWorkInstructionId(wi.getPersistentId());
				event.setLocation(wi.getPickInstruction());
				OrderDetail orderDetail = wi.getOrderDetail();
				if (orderDetail != null) {
					event.setOrderDetailId(orderDetail.getPersistentId());
				}
			}
			
			event.generateDomainId();
			WorkerEvent.staticGetDao().store(event);
			TenantPersistenceService.getInstance().commitTransaction();
			save_completed = true;
		} finally {
			if(!save_completed) {
				TenantPersistenceService.getInstance().rollbackTransaction();
			}
		}
	}

	public List<PickRate> getPickRate(Set<String> purposes, Interval createdTime){
		return getPickRate(ImmutableSet.of(WorkerEvent.EventType.COMPLETE, WorkerEvent.EventType.SHORT),
			               ImmutableSet.of("workerId"),
			               purposes,
			               createdTime);
	}
	@SuppressWarnings("unchecked")
	public List<PickRate> getPickRate(Set<WorkerEvent.EventType> types, Set<String> groupPropertyNames, Set<String> purposes, Interval createdTime){
		Preconditions.checkArgument(groupPropertyNames.size() > 0, "must group pick rates by at least one property");
		List<String> selectProperties = new ArrayList<>();
		for (String groupPropertyName : groupPropertyNames) {
			String selectClause = String.format("%s as %s", groupPropertyName, groupPropertyName);
			selectProperties.add(selectClause);
		}
		String selectClause = Joiner.on(",").join(selectProperties);
		String groupByClause = Joiner.on(",").join(groupPropertyNames);
		Session session = TenantPersistenceService.getInstance().getSession();
		Query query = session.createQuery("SELECT "
				+                    selectClause
				+ "                 ,HOUR(created) as hour"
				+ "                 ,count(*) as picks"
				//+ "                 ,sum(workInstruction.actualQuantity) as quantity" TODO should denormalize into workerevent
				+ "           FROM WorkerEvent"
				+ "          WHERE eventType IN (:includedEventTypes)"
				+              ((purposes != null && purposes.size() > 0) ? " AND purpose IN (:purposes)" : "") 
				+ "            AND created BETWEEN :startDateTime AND :endDateTime"
				+ "       GROUP BY "
				+                  groupByClause
				+ "                ,HOUR(created)"
				+ "       ORDER BY HOUR(created)"
				);
		query.setParameterList("includedEventTypes", types);
		Timestamp startTimestamp = new Timestamp(createdTime.getStartMillis());
		Timestamp endTimestamp = new Timestamp(createdTime.getEndMillis());
		query.setParameter("startDateTime", startTimestamp); //use setParameter instead of set timestamp so that it goes through the UTC conversion before hitting db
		query.setParameter("endDateTime", endTimestamp);
		if (purposes != null && purposes.size() > 0) {
			query.setParameterList("purposes", purposes);
		}
		query.setResultTransformer(new AliasToBeanResultTransformer(PickRate.class));
		List<PickRate> results = query.list();
 		return results;
	}

	@ToString
	public static class WorkerEventTypeGroup {
		@Getter @Setter
		WorkerEvent.EventType eventType;
		
		@Getter @Setter
		long count;
	}

	@SuppressWarnings("unchecked")
	public List<WorkerEventTypeGroup> groupWorkerEventsByType(Facility facility, Interval createdInterval, boolean resolved) {
        Criteria criteria = WorkerEvent.staticGetDao().createCriteria();
        criteria.setProjection(Projections.projectionList()
        		.add(Projections.groupProperty("eventType"), "eventType")
        		.add(Projections.rowCount(), "count"))
        	.add(Restrictions.eq("parent", facility))
        	.add(GenericDaoABC.createIntervalRestriction("created", createdInterval));
        	if (resolved){
        		criteria.add(Restrictions.isNotNull("resolution"));
        	} else {
        		criteria.add(Restrictions.isNull("resolution"));
        	}

        	criteria.setResultTransformer(new AliasToBeanResultTransformer(WorkerEventTypeGroup.class));
        	return criteria.list();
    }

	public List<String> getDistinct(Facility facility, String name) {
		Set<WorkerEvent.EventType> types = ImmutableSet.of(EventType.COMPLETE, EventType.SHORT);
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) WorkerEvent.staticGetDao().createCriteria()
			.add(Property.forName("parent").eq(facility))
			.add(Property.forName("eventType").in(types))
			.setProjection(Projections.distinct(Property.forName(name)))
			.list();
		return result;
	}
}