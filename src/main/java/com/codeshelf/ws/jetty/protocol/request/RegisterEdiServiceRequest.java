package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class RegisterEdiServiceRequest extends RequestABC {
	
	 @Getter @Setter
	 String facilityId;
	 
	 @Getter @Setter
	 String serviceProvider;
}
