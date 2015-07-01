package com.codeshelf.api.responses;

import java.sql.Timestamp;
import java.util.UUID;

import lombok.Getter;

import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.google.common.base.Strings;

public class EventDisplay {
	//Event Fields
	@Getter
	private UUID 							persistentId;

	@Getter
	private WorkerEvent.EventType 						type;

	@Getter
	private String itemId;

	@Getter
	private String itemUom;

	@Getter
	private String itemDescription;


	@Getter
	private String itemLocation;

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
		WorkInstruction wi = null;
		
		UUID workInstructionId = event.getWorkInstructionId();
		if (workInstructionId != null) {
			wi = WorkInstruction.staticGetDao().findByPersistentId(workInstructionId);
		}
		
		Worker worker = Worker.findWorker(event.getFacility(), event.getWorkerId());
		return new EventDisplay(event, wi, worker);
	}

	private EventDisplay(WorkerEvent event, WorkInstruction wi, Worker worker) {
		if (wi != null) {
			itemId = wi.getItemId();
			itemDescription = wi.getItemMaster().getDescription();
			itemUom = wi.getUomMasterId();
			itemLocation = wi.getPickInstruction();
			wiPlanQuantity = wi.getPlanQuantity();
			wiActualQuantity = wi.getActualQuantity();
			orderId = wi.getOrderId();
			deviceGuid = wi.getAssignedChe().getDeviceGuidStr();
		}

		if (worker != null) {
			workerName = String.format("%s, %s %s", Strings.nullToEmpty(worker.getLastName()),
				Strings.nullToEmpty(worker.getFirstName()),
				Strings.nullToEmpty(worker.getMiddleInitial()));
		}
		
		persistentId = event.getPersistentId();
		type = event.getEventType();
		createdAt = event.getCreated();
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
