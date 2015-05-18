package com.codeshelf.api;

import java.util.Date;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.NotificationService.EventType;
import com.codeshelf.util.DateTimeParser;

public abstract class BaseResponse {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(BaseResponse.class);

	
	@Setter
	private Status	status	= Status.OK;

	public Response buildResponse() {
		return buildResponse(this, status);
	}

	public static Response buildResponse(Object obj) {
		return buildResponse(obj, Status.OK);
	}

	
	public static Response buildResponse(Object obj, Status status) {
		ResponseBuilder builder = Response.status(status);
		if (obj != null) {
			builder = builder.entity(obj);
		}
		return builder.build();
	}

	public static Response buildResponse(Object obj, Status status, MediaType type) {
		ResponseBuilder builder = Response.status(status);
		if (obj != null) {
			builder = builder.entity(obj);
		}
		builder.type(type);
		return builder.build();
	}

	public static boolean isUUIDValid(UUIDParam uuid, String paramName, ErrorResponse errors) {
		if (uuid == null) {
			errors.setStatus(Status.BAD_REQUEST);
			errors.addErrorMissingQueryParam(paramName);
			return false;
		} else {
			UUID facilityId = uuid.getValue();
			if (facilityId == null) {
				errors.setStatus(Status.BAD_REQUEST);
				errors.addErrorBadUUID(uuid.getRawValue());
				return false;
			}
		}
		return true;
	}

	public static abstract class AbstractParam<T> {
		private String	raw;
		private T value;

		public AbstractParam(String str) {
			raw = str;
			try {
				value = parse(str);
			} catch (Exception e) {
				LOGGER.warn("parameter parsing problem for {}", str, e);
				throw new WebApplicationException(onError(str, e));

			}
		}
		
		protected abstract T parse(String param); 

		public String getRawValue() {
			return raw;
		}

		public T getValue() {
			return value;
		}

		public String toString() {
			return getValue().toString();
		}

		protected Response onError(String param, Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST)
						.entity(getErrorMessage(param, e))
						.build();
		}

		protected String getErrorMessage(String param, Throwable e) {
			return "Invalid parameter: " + param + " (" + e.getMessage() + ")";
		}
	}
	
	public static class UUIDParam extends AbstractParam<UUID> {

		public UUIDParam(String str) {
			super(str);
		}

		@Override
		protected UUID parse(String param) {
			return UUID.fromString(param);
		}
		
	}
	
	public static class StartDateParam extends AbstractParam<Date> {

		public StartDateParam(String str) {
			super(str);
		}

		@Override
		protected Date parse(String param) { 
			final DateTimeParser dateParser = new DateTimeParser();
			return dateParser.parse(param, DateTimeParser.UnspecifiedTime.START_OF_DAY);
		}
		
	}

	public static class EndDateParam extends AbstractParam<Date> {

		public EndDateParam(String str) {
			super(str);
		}

		@Override
		protected Date parse(String param) { 
			final DateTimeParser dateParser = new DateTimeParser();
			return dateParser.parse(param, DateTimeParser.UnspecifiedTime.END_OF_DAY);
		}		
	}
	
	public static class EventTypeParam extends AbstractParam<EventType> {

		public EventTypeParam(String str) {
			super(str);
		}

		@Override
		protected EventType parse(String param) { 
			return EventType.valueOf(param);
		}		
	}	
}
