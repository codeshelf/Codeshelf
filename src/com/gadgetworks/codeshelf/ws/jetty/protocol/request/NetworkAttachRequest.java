package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class NetworkAttachRequest extends RequestABC {

	@Getter @Setter
	String organizationId;

	@Getter @Setter
	String facilityId;

	@Getter @Setter
	String networkId;
	
	@Getter @Setter
	String credential;
}
