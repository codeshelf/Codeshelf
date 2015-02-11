package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.domain.PathSegment;

public class CreatePathRequest extends RequestABC {
	
	@Getter @Setter
	String facilityId;
	
	@Getter @Setter
	String domainId;
	
	@Getter @Setter
	PathSegment[] pathSegments;
}
