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
	
	public ComputeWorkRequest() {
	}
	
	public ComputeWorkRequest(ComputeWorkPurpose purpose, String cheId, String locationId, Map<String, String> positionToContainerMap, Boolean reversePickOrder) {
		this.purpose = purpose;
		setDeviceId(cheId);
		this.locationId = locationId;
		this.positionToContainerMap = positionToContainerMap;
		this.reversePickOrder = reversePickOrder;
	}
}
