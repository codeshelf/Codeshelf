package com.gadgetworks.codeshelf.api.exceptions;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.api.ErrorResponse;

@Provider
public class GenericExceptionHandler implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable ex) {
    	ErrorResponse error = new ErrorResponse();
    	error.processException(ex);
    	error.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return error.buildResponse();
    }
}