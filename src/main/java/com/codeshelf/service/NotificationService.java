package com.codeshelf.service;

import java.util.List;
import java.util.UUID;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.google.inject.Inject;

public class NotificationService implements IApiService{
	public enum EventType {LOGIN, SKIP_ITEM_SCAN, BUTTON, WI, SHORT, SHORT_AHEAD, COMPLETE, CANCEL_PUT}	
	
	private static final Logger			LOGGER				= LoggerFactory.getLogger(NotificationService.class);
	
	@Inject
	public NotificationService() {
	}

	public void saveEvent(NotificationMessage message) {
		//Is this the correct current approach to transactions?
		try {
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
			throw e;
		} finally {
			TenantPersistenceService.getInstance().commitTransaction();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getPickSummary(){
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		Session session = TenantPersistenceService.getInstance().getSession();
		String schema = tenant.getSchemaName();

		String queryStr = String.format(
			"SELECT " +
			"	e.worker_id AS worker_id,\n"+
			"	count(*) AS num_picks,\n" +
			"	SUM(wi.actual_quantity) AS quantity,\n" +
			"	DATE_TRUNC('hour', e.created) AS hour\n" +  
			"FROM %s.event_worker e\n" + 
			"	LEFT JOIN %s.work_instruction wi ON e.work_instruction_persistentid = wi.persistentid\n" + 
			"WHERE e.event_type = 'COMPLETE'					--Only loop at Pick actions\n" + 
			"	AND e.work_instruction_persistentid IS NOT NULL --Skip housekeeping actions\n" + 
			"GROUP BY e.worker_id, date_trunc('hour', e.created)\n" +
			"ORDER BY date_trunc('hour', e.created)", schema, schema);
		SQLQuery getPickSummaryQuery = session.createSQLQuery(queryStr)
			.addScalar("worker_id", StandardBasicTypes.STRING)
			.addScalar("num_picks", StandardBasicTypes.INTEGER)
			.addScalar("quantity", StandardBasicTypes.INTEGER)
			.addScalar("hour", StandardBasicTypes.TIMESTAMP);
		getPickSummaryQuery.setCacheable(true);
		List<Object[]> pickSummary = getPickSummaryQuery.list();
		pickSummary = null;
	}
}