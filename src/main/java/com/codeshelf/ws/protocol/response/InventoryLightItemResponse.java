package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class InventoryLightItemResponse extends ResponseABC {

	@Getter
	@Setter
	Boolean foundLocation;
	
	@Getter
	@Setter
	Boolean foundGtin;
	
	public void appendStatusMessage(String inMessage) {
		this.setStatusMessage(this.getStatusMessage() + inMessage);
	}
}