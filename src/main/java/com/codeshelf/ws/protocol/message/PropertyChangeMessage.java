package com.codeshelf.ws.protocol.message;

import com.codeshelf.model.FacilityPropertyType;

import lombok.Getter;

public class PropertyChangeMessage extends MessageABC{
	@Getter
	private FacilityPropertyType type;
	
	@Getter
	private String value;
	
	public PropertyChangeMessage() {
	}
	
	public PropertyChangeMessage(FacilityPropertyType type, String value) {
		this.type = type;
		this.value = value;
	}
	
	@Override
	public String getDeviceIdentifier() {
		return null;
	}

}
