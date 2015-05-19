package com.codeshelf.api.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.codeshelf.api.ErrorResponse;

@Provider
public class GenericExceptionHandler implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable ex) {
    	ErrorResponse error = new ErrorResponse();
    	error.processException(ex);
    	error.setStatus(Status.INTERNAL_SERVER_ERROR);
        return error.buildResponse();
    }
}