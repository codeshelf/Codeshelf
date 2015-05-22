package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.responses.PickRate;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.ws.protocol.message.NotificationMessage;
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
				LOGGER.error("Notification service doesn't know how to process notifications from " + deviceClass + " devices");
				return;
			}
			if (device == null) {
				LOGGER.error(String.format("Notification service unable to find %s device %s (%s)", deviceClass, message.getDevicePersistentId(), message.getDeviceGuid()));
				return;
			}
			
			WorkerEvent event = new WorkerEvent();
			event.setCreated(new Timestamp(message.getTimestamp()));
			event.setFacility(device.getFacility());
			event.setEventType(message.getEventType());
			event.setDevicePersistentId(message.getDevicePersistentId().toString());
			event.setDeviceGuid(message.getDeviceGuid());
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
	public List<PickRate> getPickRate(Date startDate, Date endDate){
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		Session session = TenantPersistenceService.getInstance().getSession();
		String schemaName = tenant.getSchemaName();

		String dateCondition = startDate == null ? "" : "AND e.created >= '" + new Timestamp(startDate.getTime()) + "'";
		String queryStr = String.format(
			"SELECT " +
			"	e.worker_id AS worker_id,\n"+
			"	count(*) AS num_picks,\n" +
			"	SUM(wi.actual_quantity) AS quantity,\n" +
			"	DATE_TRUNC('hour', e.created) AS hour\n" +  
			"FROM %s.event_worker e\n" + 
			"	LEFT JOIN %s.work_instruction wi ON e.work_instruction_persistentid = wi.persistentid\n" + 
			"WHERE e.event_type = 'COMPLETE'\n" +																//Only look at Pick actions 
			"	AND e.work_instruction_persistentid IS NOT NULL\n" + 											//Skip housekeeping actions
			"	%s\n" +																							//Filter by earliest data when needed
			"GROUP BY e.worker_id, date_trunc('hour', e.created)\n" +
			"ORDER BY date_trunc('hour', e.created)", schemaName, schemaName, dateCondition);
		SQLQuery getPickSummaryQuery = session.createSQLQuery(queryStr)
			.addScalar("worker_id", StandardBasicTypes.STRING)
			.addScalar("hour", StandardBasicTypes.TIMESTAMP)
			.addScalar("hour", StandardBasicTypes.STRING)
			.addScalar("num_picks", StandardBasicTypes.INTEGER)
			.addScalar("quantity", StandardBasicTypes.INTEGER);
		getPickSummaryQuery.setCacheable(true);
		List<Object[]> pickSummary = getPickSummaryQuery.list();
		List<PickRate> result = Lists.newArrayList();
		if (pickSummary != null) {
			for (Object[] item : pickSummary) {
				String workerId = (String) item[0];
				Timestamp hour = (Timestamp) item[1];
				String hourUI = (String) item[2];
				Integer picks = (Integer) item[3];
				Integer quantity = (Integer) item[4];
				PickRate rate = new PickRate(workerId, hour, hourUI, picks, quantity);
				result.add(rate);
			}
		}
		return result;
	}
}