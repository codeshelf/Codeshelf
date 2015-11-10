package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class FacilityPropertiesRequest extends RequestABC {
	@Getter @Setter
	String persistentId;	
}
