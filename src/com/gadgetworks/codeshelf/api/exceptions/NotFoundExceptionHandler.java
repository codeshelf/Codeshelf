package com.gadgetworks.codeshelf.api.exceptions;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gadgetworks.codeshelf.api.ErrorResponse;
import com.sun.jersey.api.NotFoundException;

@Provider
public class NotFoundExceptionHandler implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException ex) {
    	ErrorResponse error = new ErrorResponse();
    	error.processException(ex);
    	error.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return error.buildResponse();
    }
}