package com.codeshelf.ws.jetty.protocol.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.ws.jetty.protocol.command.ArgsClass;

/* {"className":"Che",
 * "persistentId":"66535fc0-00b8-11e4-ba3a-48d705ccef0f",
 * "methodName":"changeControllerId",
 * "methodArgs":[{"name":"inNewControllerId","value":"0x08009991","classType":"java.lang.String"}]}}
*/

public class ObjectMethodRequest extends RequestABC {
	
	@Getter @Setter
	String className;
	
	@Getter @Setter
	String persistentId;
	
	@Getter @Setter
	String methodName;
	
	@Getter @Setter
	List<ArgsClass> methodArgs;
}
