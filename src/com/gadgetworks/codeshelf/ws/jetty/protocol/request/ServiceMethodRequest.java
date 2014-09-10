package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ArgsClass;

/* {"className":"WorkService",
 * "methodName":"wiSummaryForChe",
 * "methodArgs":["3039a670-33b1-11e4-a7ed-6673094fa2cf}
*/

public class ServiceMethodRequest extends RequestABC {
	
	@Getter @Setter
	String className;
	
	
	@Getter @Setter
	String methodName;
	
	@Getter @Setter
	List<?> methodArgs;
	
	public ServiceMethodRequest() {
	}
	
	public ServiceMethodRequest(String className, String methodName, List<?> methodArgs) {
		setClassName(className);
		setMethodName(methodName);
		setMethodArgs(methodArgs);
	}

}
