package com.codeshelf.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.Event;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.persistence.TenantPersistenceService;
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
			
			Event event = new Event();
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
			Event.staticGetDao().store(event);
		} catch (Exception e) {
			throw e;
		} finally {
			TenantPersistenceService.getInstance().commitTransaction();
		}
	}
}
