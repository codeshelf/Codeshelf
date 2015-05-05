package com.codeshelf.api.responses;

import java.sql.Timestamp;
import java.util.UUID;

import lombok.Getter;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.service.NotificationService.EventType;
import com.google.common.base.Strings;

public class EventDisplay {
	//Event Fields
	@Getter
	private UUID 							eventId;
	
	@Getter
	private EventType 						type;

	@Getter
	private String itemId;
	
	@Getter
	private String itemUom;
	
	@Getter
	private String itemDescription;

	@Getter
	private Integer wiPlanQuantity;
	
	@Getter
	private Integer wiActualQuantity;
	
	@Getter
	private String workerName;
		
	@Getter
	private String	orderId;

	@Getter
	private String deviceGuid;
	
	@Getter
	private Timestamp 						createdAt;
	
	@Getter
	private String							devicePersistentId;
	
	@Getter
	private String							workerId;
	
	@Getter
	private UUID							orderDetailId;

	@Getter
	private UUID							workInstructionId;

	//Resolution Fields
	@Getter
	private Boolean resolved = false;
	
	@Getter
	private Timestamp resolvedAt;
	
	@Getter
	private String resolvedBy;

	public static EventDisplay createEventDisplay(WorkerEvent event) {
		WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(event.getWorkInstructionId());
		Worker worker = Worker.findWorker(event.getFacility(), event.getWorkerId());
		return new EventDisplay(event, wi, worker);
	}
	
	private EventDisplay(WorkerEvent event, WorkInstruction wi, Worker worker) {
		eventId = event.getPersistentId();
		type = event.getEventType();
		createdAt = event.getCreated();
		itemId = wi.getItemId();
		itemDescription = wi.getItemMaster().getDescription();
		itemUom = wi.getUomMasterId();
		wiPlanQuantity = wi.getPlanQuantity();
		wiActualQuantity = wi.getActualQuantity();
		workerName = String.format("%s, %s %s", Strings.nullToEmpty(worker.getLastName()),
												Strings.nullToEmpty(worker.getFirstName()),
												Strings.nullToEmpty(worker.getMiddleInitial()));
		orderId = wi.getOrderId();
		deviceGuid = wi.getAssignedChe().getDeviceGuidStr();
		devicePersistentId = event.getDevicePersistentId();
		workerId = event.getWorkerId();
		orderDetailId = event.getOrderDetailId();
		workInstructionId = event.getWorkInstructionId();
		Resolution resolution = event.getResolution();
		if (resolution != null){
			resolved = true;
			resolvedAt = resolution.getTimestamp();
			resolvedBy = resolution.getResolvedBy();					
		}
	}
}