package com.codeshelf.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import lombok.Getter;

public abstract class CodeshelfHealthCheck extends HealthCheck {

	public static final String	OK	= "OK";
	
	@Getter
	String name;
	
	public CodeshelfHealthCheck(String name) {
		this.name = name;
	}
	
	protected Result unhealthy(String message) {
		Logger	LOGGER						= LoggerFactory.getLogger(this.getClass());
		LOGGER.warn("Healthcheck unhealthy: {}", message);
		return Result.unhealthy(message);
	}

	protected Result unhealthy(String messageFormat, Object... args) {
		Logger	LOGGER						= LoggerFactory.getLogger(this.getClass());
		LOGGER.warn("Healthcheck unhealthy: {}", String.format(messageFormat, args));
		return Result.unhealthy(messageFormat, args);
	}
		
	protected Result unhealthy(Throwable throwable) {
		Logger	LOGGER						= LoggerFactory.getLogger(this.getClass());
		LOGGER.warn("Healthcheck unhealthy", throwable);
		return Result.unhealthy(throwable);
	}
	
}