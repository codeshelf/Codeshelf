package com.codeshelf.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import lombok.Getter;

public abstract class CodeshelfHealthCheck extends HealthCheck {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CodeshelfHealthCheck.class);
	public static final String	OK		= "OK";

	@Getter
	String						name;

	public CodeshelfHealthCheck(String name) {
		this.name = name;
	}

	// No need to spam our logs if (as for production value) we return false but it is ok.
	protected Result unhealthy(String message) {
		if (OK.equals(message)) {
		} else {
			LOGGER.warn("{} healthcheck unhealthy: {}", getName(), message);
		}
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