package com.codeshelf.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import lombok.Getter;

public abstract class CodeshelfHealthCheck extends HealthCheck {
	private static final Logger		LOGGER	= LoggerFactory.getLogger(CodeshelfHealthCheck.class);
	public static final String	OK	= "OK";
	
	@Getter
	String name;
	
	public CodeshelfHealthCheck(String name) {
		this.name = name;
	}
	
	protected Result unhealthy(String message) {
		LOGGER.warn("Healthcheck unhealthy: {}", message);
		return Result.unhealthy(message);
	}

	protected Result unhealthy(String messageFormat, Object... args) {
		LOGGER.warn("Healthcheck unhealthy: {}", String.format(messageFormat, args));
		return Result.unhealthy(messageFormat, args);
	}
		
	protected Result unhealthy(Throwable throwable) {
		LOGGER.warn("Healthcheck unhealthy", throwable);
		return Result.unhealthy(throwable);
	}
	
}