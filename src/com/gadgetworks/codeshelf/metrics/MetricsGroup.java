package com.gadgetworks.codeshelf.metrics;

import lombok.Getter;

public enum MetricsGroup {
	JVM("jvm"),
	Database("database"),
	WSS("wss"), 
	EDI("edi"), 
	Radio("radio");
	
	@Getter
	String name;
	
	private MetricsGroup(String name) {
		this.name = name;
	}
}
