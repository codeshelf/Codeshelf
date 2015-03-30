package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class RegisterEdiServiceRequest extends RequestABC {
	
	 @Getter @Setter
	 String facilityId;
	 
	 @Getter @Setter
	 String serviceProvider;
}
