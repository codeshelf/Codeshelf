package com.codeshelf.ws.protocol.request;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class RegisterFilterRequest extends RequestABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	List<String> propertyNames;
	
	@Getter @Setter
	String filterClause;
	
	@Getter @Setter
	List<Map<String, Object>> filterParams;
}
