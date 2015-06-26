package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class InventoryUpdateResponse extends ResponseABC {

	@Getter
	@Setter
	Boolean foundGtin;
	
	@Getter
	@Setter
	Boolean foundLocation;
	
	public void appendStatusMessage(String inMessage) {
		this.setStatusMessage(this.getStatusMessage() + inMessage);
	}
}
