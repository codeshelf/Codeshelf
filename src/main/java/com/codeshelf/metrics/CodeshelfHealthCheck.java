package com.codeshelf.metrics;

import lombok.Getter;

import com.codahale.metrics.health.HealthCheck;

public abstract class CodeshelfHealthCheck extends HealthCheck {

	public static final String	OK	= "OK";
	
	@Getter
	String name;
	
	public CodeshelfHealthCheck(String name) {
		this.name = name;
	}
}