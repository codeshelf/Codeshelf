package com.gadgetworks.codeshelf.api;

import java.util.ArrayList;

import javassist.NotFoundException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
@JsonInclude(Include.NON_NULL)
public class ErrorResponse extends BaseResponse{
	@Getter
	private ArrayList<String> errors;

	public void addError(String error) {
		if (errors == null) {errors = new ArrayList<>();}
		errors.add(error);
	}
	
	public void addErrorMissingQueryParam(String param) {
		addError("Missing query param '" + param + "'");
	}

	public void addErrorBadUUID(String uuid) {
		addError("Could not parse uuid " + uuid);
	}
	
	public void addErrorUUIDDoesntExist(String uuid, String type) {
		addError("Could not find " + type + " " + uuid);
	}
	
	public void processException(Exception e) {
		String message = e.getMessage();
		if (message == null || message.isEmpty()) {
			message = ExceptionUtils.getStackTrace(e);
		}
		addError(message);
		if (e instanceof NotFoundException) {
			setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}