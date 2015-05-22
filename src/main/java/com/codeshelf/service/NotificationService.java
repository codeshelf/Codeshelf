package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.TimestampType;
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
import com.codeshelf.persistence.UtcTimestampType;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class NotificationService implements IApiService{
	
	public enum EventType {
		LOGIN, SKIP_ITEM_SCAN, BUTTON, WI, SHORT, SHORT_AHEAD, COMPLETE, CANCEL_PUT;
		
		public String getName() {
			return name();
		}
	}	
	
	private static final Logger			LOGGER				= LoggerFactory.getLogger(NotificationService.class);
	
	private static final EnumSet<EventType> SAVE_ONLY = EnumSet.of(EventType.SKIP_ITEM_SCAN, EventType.SHORT, EventType.COMPLETE);
	
	@Inject
	public NotificationService() {
	}
	
	public void saveEvent(WorkerEvent event) {
		WorkerEvent.staticGetDao().store(event);
	}

	public void saveEvent(NotificationMessage message) {
		//Is this the correct current approach to transactions?
		try {
			if (!SAVE_ONLY.contains(message.getEventType())) {
				return;
			}
			TenantPersistenceService.getInstance().beginTransaction();
			LOGGER.info("Saving notification from {}: {}", message.getDeviceGuid(), message.getEventType());
			Class<?> deviceClass = message.getDeviceClass();
			DomainObjectABC device = null;
			if (deviceClass == Che.class) {
				device = Che.staticGetDao().findByPersistentId(message.getDevicePersistentId());
			} else {
				throw new IllegalArgumentException("unable to process notifications from " + deviceClass + " devices");
			}
			if (device == null) {
				throw new IllegalArgumentException(String.format("unable to find %s device %s (%s)", deviceClass, message.getDevicePersistentId(), message.getDeviceGuid()));
			}
			
			WorkerEvent event = new WorkerEvent();
			event.setCreated(new Timestamp(message.getTimestamp()));
			event.setFacility(device.getFacility());
			event.setEventType(message.getEventType());
			event.setDeviceGuid(new NetGuid(message.getDeviceGuid()).toString());
			event.setWorkerId(message.getWorkerId());
			
			UUID workInstructionId = message.getWorkInstructionId();
			if (workInstructionId != null) {
				event.setWorkInstructionId(workInstructionId);
				WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(workInstructionId);
				OrderDetail orderDetail = wi.getOrderDetail();
				if (orderDetail != null) {
					event.setOrderDetailId(orderDetail.getPersistentId());
				}
			}
			
			event.generateDomainId();
			WorkerEvent.staticGetDao().store(event);
		} catch (Exception e) {
			TenantPersistenceService.getInstance().rollbackTransaction();
			throw e;
		} finally {
			TenantPersistenceService.getInstance().commitTransaction();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<PickRate> getPickRate(DateTime startDateTime, DateTime endDateTime){
		Session session = TenantPersistenceService.getInstance().getSession();
		Query query = session.createQuery("SELECT workerId as workerId"
				+ "                 ,HOUR(created) as hour"
				+ "                 ,count(*) as picks"
				+ "                 ,sum(workInstruction.actualQuantity) as quantity"
				+ "           FROM WorkerEvent"
				+ "          WHERE eventType IN (:includedEventTypes)"
				+ "            AND created BETWEEN :startDateTime AND :endDateTime"
				+ "       GROUP BY workerId,"
				+ "                HOUR(created)"
				+ "       ORDER BY HOUR(created)"
				);
		query.setParameterList("includedEventTypes", ImmutableList.of(EventType.COMPLETE, EventType.SHORT));
		query.setParameter("startDateTime", new Timestamp(startDateTime.getMillis()));
		query.setParameter("endDateTime", new Timestamp(endDateTime.getMillis()));
		query.setResultTransformer(new AliasToBeanResultTransformer(PickRate.class));
		List<PickRate> results = query.list();
		return results;
	}
}