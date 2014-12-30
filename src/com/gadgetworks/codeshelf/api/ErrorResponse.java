package com.gadgetworks.codeshelf.api;

import java.util.ArrayList;

import javassist.NotFoundException;

import javax.servlet.http.HttpServletResponse;

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
	
	public void processException(Exception e) {
		addError(e.getMessage());
		if (e instanceof NotFoundException) {
			setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}