package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.responses.PickRate;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class NotificationService implements IApiService{
	
	private static final Logger			LOGGER				= LoggerFactory.getLogger(NotificationService.class);
	
	private static final EnumSet<WorkerEvent.EventType> SAVE_ONLY = EnumSet.of(WorkerEvent.EventType.SKIP_ITEM_SCAN, WorkerEvent.EventType.SHORT, WorkerEvent.EventType.COMPLETE);
	
	@Inject
	public NotificationService() {
	}
	
	public WorkerEvent saveEvent(WorkerEvent event) {
		WorkerEvent.staticGetDao().store(event);
		return event;
	}

	public void saveEvent(NotificationMessage message) {
		if (!SAVE_ONLY.contains(message.getEventType())) {
			return;
		}
		boolean save_completed=false;
		try {
			TenantPersistenceService.getInstance().beginTransaction();
			LOGGER.info("Saving notification from {}: {}", message.getNetGuidStr(), message.getEventType());
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
			event.setFacility(device.getFacility());
			event.setDevicePersistentId(device.getPersistentId().toString());
			
			event.setCreated(new Timestamp(message.getTimestamp()));
			event.setEventType(message.getEventType());
			event.setWorkerId(message.getWorkerId());
			
			UUID workInstructionId = message.getWorkInstructionId();
			if (workInstructionId != null) {
				WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(workInstructionId);
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
}