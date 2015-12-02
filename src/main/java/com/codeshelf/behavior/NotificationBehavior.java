package com.codeshelf.behavior;

import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.responses.PickRate;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

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
		
		event.setCreated(wi.getCompleted());
		event.setEventType(type);
		event.setWorkerId(wi.getPickerId());
		event.setWorkInstruction(wi);
		event.setWorkInstruction(wi);
		OrderDetail orderDetail = wi.getOrderDetail();
		if (orderDetail != null) {
			event.setOrderDetailId(orderDetail.getPersistentId());
		}
		event.generateDomainId();
		WorkerEvent.staticGetDao().store(event);
		
		//Save Complete or Short event into WorkerHourlyMetric
		workerHourlyMetricBehavior.recordEvent(wi.getFacility(), wi.getPickerId(), type);
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
				event.setWorkInstruction(wi);
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
	
	@SuppressWarnings("unchecked")
	public List<PickRate> getPickRate(DateTime startDateTime, DateTime endDateTime){
		Session session = TenantPersistenceService.getInstance().getSession();
		Query query = session.createQuery("SELECT workerId as workerId"
				+ "                 ,HOUR(created) as hour"
				+ "                 ,count(*) as picks"
				//+ "                 ,sum(workInstruction.actualQuantity) as quantity" TODO should denormalize into workerevent
				+ "           FROM WorkerEvent"
				+ "          WHERE eventType IN (:includedEventTypes)"
				+ "            AND created BETWEEN :startDateTime AND :endDateTime"
				+ "       GROUP BY workerId,"
				+ "                HOUR(created)"
				+ "       ORDER BY HOUR(created)"
				);
		query.setParameterList("includedEventTypes", ImmutableList.of(WorkerEvent.EventType.COMPLETE, WorkerEvent.EventType.SHORT));
		Timestamp startTimestamp = new Timestamp(startDateTime.getMillis());
		Timestamp endTimestamp = new Timestamp(endDateTime.getMillis());
		query.setParameter("startDateTime", startTimestamp); //use setParameter instead of set timestamp so that it goes through the UTC conversion before hitting db
		query.setParameter("endDateTime", endTimestamp);
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
	public List<WorkerEventTypeGroup> groupWorkerEventsByType(Facility facility, boolean resolved) {
        Criteria criteria = WorkerEvent.staticGetDao().createCriteria();
        criteria.setProjection(Projections.projectionList()
        		.add(Projections.groupProperty("eventType"), "eventType")
        		.add(Projections.rowCount(), "count"))
        	.add(Restrictions.eq("facility", facility));
        	if (resolved){
        		criteria.add(Restrictions.isNotNull("resolution"));
        	} else {
        		criteria.add(Restrictions.isNull("resolution"));
        	}

        	criteria.setResultTransformer(new AliasToBeanResultTransformer(WorkerEventTypeGroup.class));
        	return criteria.list();
    }
}