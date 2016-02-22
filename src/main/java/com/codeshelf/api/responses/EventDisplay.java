package com.codeshelf.api.responses;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.Getter;

public class EventDisplay {

	private static Cache<String, Worker> cache = CacheBuilder.newBuilder()
			.maximumSize(500)
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.build();

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
	private String itemGtin;

	@Getter
	private Integer wiPlanQuantity;

	@Getter
	private Integer wiActualQuantity;

	@Getter
	private String workerName;

	@Getter
	private String	orderId;


	@Getter
	private String deviceName;

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

		Che che = null;
		String devicePersistentId = Strings.emptyToNull(event.getDevicePersistentId());
		if (devicePersistentId != null) {
			UUID uuid = UUID.fromString(devicePersistentId);
			che = Che.staticGetDao().findByPersistentId(uuid);
		}
		
		//Stopwatch findWorker = Stopwatch.createStarted();
		String key = event.getFacility().getDomainId()+event.getWorkerId();
		Worker worker = cache.getIfPresent(key);
		if(worker == null) {
			worker = Worker.findWorker(event.getFacility(), event.getWorkerId());
			cache.put(key,  worker);

		}

		return new EventDisplay(event, wi, worker, che);
	}

	private EventDisplay(WorkerEvent event, WorkInstruction wi, Worker worker, Che che) {
		Che cheToUse = che;
		if (wi != null) {
			itemId = wi.getItemId();
			itemDescription = wi.getItemMaster().getDescription();
			itemUom = wi.getUomMasterId();
			itemLocation = wi.getPickInstruction();
			itemGtin = wi.getGtin();
			wiPlanQuantity = wi.getPlanQuantity();
			wiActualQuantity = wi.getActualQuantity();
			orderId = wi.getOrderId();
			cheToUse = MoreObjects.firstNonNull(cheToUse, wi.getAssignedChe());
		}

		if (worker != null) {
			workerName = String.format("%s, %s %s", Strings.nullToEmpty(worker.getLastName()),
				Strings.nullToEmpty(worker.getFirstName()),
				Strings.nullToEmpty(worker.getMiddleInitial()));
		}

		if (cheToUse != null) {
			deviceGuid = che.getDeviceGuidStr();
			devicePersistentId = event.getDevicePersistentId();
			deviceName = che.getDomainId();
		}

		persistentId = event.getPersistentId();
		type = event.getEventType();
		createdAt = event.getCreated();
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
