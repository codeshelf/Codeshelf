package com.gadgetworks.codeshelf.apiresources.exceptions;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gadgetworks.codeshelf.apiresources.BaseResponse;

@Provider
public class GenericExceptionHandler implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable ex) {
        return BaseResponse.buildResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}