package com.codeshelf.ws.protocol.request;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class ComputeWorkRequest extends DeviceRequest {
	public enum ComputeWorkPurpose {COMPUTE_WORK, GET_WORK}
	
	@Getter
	private ComputeWorkPurpose purpose;
	
	@Getter
	private String locationId;

	@Getter @Setter
	private Map<String, String>	positionToContainerMap;
	
	@Getter @Setter
	private Boolean reversePickOrder = false;
	
	@Getter @Setter
	private Boolean reversePickOrderFromLastTime = false;
	
	public ComputeWorkRequest() {
	}
	
	public ComputeWorkRequest(String cheId, Map<String, String>	positionToContainerMap, Boolean reversePickOrder) {
		this.purpose = ComputeWorkPurpose.COMPUTE_WORK;
		setDeviceId(cheId);
		this.positionToContainerMap = positionToContainerMap;
		this.reversePickOrder = reversePickOrder;
	}
	
	public ComputeWorkRequest(String cheId, String locationId, Boolean reversePickOrder, Boolean reversePickOrderFromLastTime) {
		this.purpose = ComputeWorkPurpose.GET_WORK;
		setDeviceId(cheId);
		this.locationId = locationId;
		this.reversePickOrder = reversePickOrder;
		this.reversePickOrderFromLastTime = reversePickOrderFromLastTime;
	}

}
