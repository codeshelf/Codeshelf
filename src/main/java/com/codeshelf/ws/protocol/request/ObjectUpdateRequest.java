package com.codeshelf.ws.protocol.request;

import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/*
 * Example Message:
 * 
 * "ObjectUpdateRequest":{
 * 		"className":"Che",
 * 		"persistentId":"66535fc0-00b8-11e4-ba3a-48d705ccef0f",
 * 		"properties":{"description":"sadasd" },
 * 		"messageId":"cid_10"
 * }
 */


public class ObjectUpdateRequest extends RequestABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	String persistentId;
	
	@Getter @Setter
	Map<String, Object> properties;
	
	public ObjectUpdateRequest() {
		super();
	}
	
	public ObjectUpdateRequest(String className, UUID persistentId, Map<String, Object> properties) {
		super();
		this.className = className;
		this.persistentId = persistentId.toString();
		this.properties = properties;
	}
}
