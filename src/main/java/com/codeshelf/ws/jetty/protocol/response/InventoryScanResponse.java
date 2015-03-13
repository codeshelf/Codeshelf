package com.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class InventoryScanResponse extends ResponseABC {

	@Getter
	@Setter
	Boolean foundGtin;
	
	@Getter
	@Setter
	Boolean foundLocation;
	
}
