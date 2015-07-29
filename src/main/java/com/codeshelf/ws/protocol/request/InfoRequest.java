package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class InfoRequest extends DeviceRequestABC{
	public enum InfoRequestType {GET_WALL_LOCATION_INFO, LIGHT_COMPLETE_ORDERS, LIGHT_INCOMPLETE_ORDERS, REMOVE_WALL_ORDERS, GET_INVENTORY_INFO}
	
	@Getter
	private InfoRequestType type;
	
	@Getter
	private String location;
	
	public InfoRequest() {
	}
	
	public InfoRequest(InfoRequestType type, String cheId, String location) {
		this.type = type;
		this.location = location;
		setDeviceId(cheId);
	}
}
