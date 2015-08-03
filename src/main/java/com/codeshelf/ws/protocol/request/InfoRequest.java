package com.codeshelf.ws.protocol.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class InfoRequest extends DeviceRequestABC{
	public enum InfoRequestType {GET_WALL_LOCATION_INFO, GET_INVENTORY_INFO, LIGHT_COMPLETE_ORDERS, LIGHT_INCOMPLETE_ORDERS, REMOVE_WALL_ORDERS, REMOVE_INVENTORY}
	
	@Getter
	private InfoRequestType type;
	
	@Getter
	private String location;
	
	@Getter @Setter
	private UUID removeItemId;
	
	public InfoRequest() {
	}
	
	public InfoRequest(InfoRequestType type, String cheId, String location, UUID removeItemId) {
		this.type = type;
		this.location = location;
		this.removeItemId = removeItemId;
		setDeviceId(cheId);
	}
}
